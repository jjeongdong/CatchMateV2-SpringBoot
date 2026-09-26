package com.back.catchmate.inquiry.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.inquiry.dto.request.InquiryCreateRequest;
import com.back.catchmate.inquiry.dto.response.AnswerDraftResponse;
import com.back.catchmate.inquiry.dto.response.InquiryCreateResponse;
import com.back.catchmate.inquiry.dto.response.InquiryDetailResponse;
import com.back.catchmate.inquiry.dto.response.InquirySummary;
import com.back.catchmate.inquiry.entity.Inquiry;
import com.back.catchmate.inquiry.entity.InquiryStatus;
import com.back.catchmate.inquiry.infra.SpringAiAssistClient;
import com.back.catchmate.inquiry.infra.dto.AnswerDraft;
import com.back.catchmate.inquiry.infra.dto.CorpusDoc;
import com.back.catchmate.inquiry.repository.InquiryRepository;
import com.back.catchmate.notice.service.NoticeService;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryService {
    // 코퍼스 색인용 일괄 조회 상한 (소규모 코퍼스 전제)
    private static final int MAX_CORPUS_FETCH = 1000;

    private final InquiryRepository inquiryRepository;
    private final SpringAiAssistClient assistClient;

    private final UserQueryApi userQueryApi;
    private final NoticeService noticeService;

    @Transactional
    public InquiryCreateResponse createInquiry(Long userId, InquiryCreateRequest request) {
        Inquiry inquiry = Inquiry.createInquiry(userId, request.type(), request.content());
        Inquiry saved = inquiryRepository.save(inquiry);
        return new InquiryCreateResponse(saved.getId(), saved.getCreatedAt());
    }

    public InquiryDetailResponse getInquiryDetail(Long userId, Long inquiryId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        if (!inquiry.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
        UserInfo inquirer = userQueryApi.getInfo(inquiry.getUserId());
        return toDetailResponse(inquiry, inquirer.nickName());
    }

    public PagedResponse<InquiryDetailResponse> getInquiryListByUser(Long userId, int page, int size) {
        Page<Inquiry> inquiryPage =
                inquiryRepository.findAllByUserId(userId, sortByCreatedAtDesc(PageRequest.of(page, size)));

        if (inquiryPage.isEmpty()) {
            return new PagedResponse<>(inquiryPage, List.of());
        }

        String nickname = userQueryApi.getInfo(userId).nickName();

        List<InquiryDetailResponse> responses = inquiryPage.getContent().stream()
                .map(i -> toDetailResponse(i, nickname))
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    // 다른 컨텍스트용 — 단건 요약
    public InquirySummary getInquirySummary(Long inquiryId) {
        return toSummary(getInquiryOrThrow(inquiryId));
    }

    // 다른 컨텍스트용 — 페이징 요약
    public Page<InquirySummary> getInquirySummaries(Pageable pageable) {
        return inquiryRepository.findAll(sortByCreatedAtDesc(pageable)).map(this::toSummary);
    }

    // 다른 컨텍스트용 — 사용자별 페이징 요약
    public Page<InquirySummary> getInquirySummariesByUser(Long userId, Pageable pageable) {
        return inquiryRepository
                .findAllByUserId(userId, sortByCreatedAtDesc(pageable))
                .map(this::toSummary);
    }

    public long getTotalInquiryCount() {
        return inquiryRepository.count();
    }

    public long getWaitingInquiryCount() {
        return inquiryRepository.countByStatus(InquiryStatus.WAITING);
    }

    @Transactional
    public void registerAnswer(Long inquiryId, String content) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        inquiry.registerAnswer(content);
        inquiryRepository.save(inquiry);
    }

    /**
     * 답변 초안 생성 — 문의 본문을 읽어 RAG 로 초안을 만든다.
     * DB 변경이 없는 읽기/생성이므로 readOnly 트랜잭션.
     */
    public AnswerDraftResponse draftAnswer(Long inquiryId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        AnswerDraft draft = assistClient.draftAnswer(inquiry.getContent());
        return new AnswerDraftResponse(draft.grounded(), draft.draftText(), draft.sources());
    }

    /**
     * 코퍼스 색인 — 공지(cross-context)와 답변완료 문의(own)를 {@link CorpusDoc} 로 모아
     * 벡터 스토어를 전량 재구축한다.
     *
     * @return 색인된 문서 수
     */
    public int reindex() {
        List<CorpusDoc> docs = new ArrayList<>();

        noticeService
                .getAllNoticeSummaries()
                .forEach(notice ->
                        docs.add(new CorpusDoc("NOTICE", notice.noticeId(), notice.title() + "\n" + notice.content())));

        inquiryRepository.findAll(sortByCreatedAtDesc(PageRequest.of(0, MAX_CORPUS_FETCH))).getContent().stream()
                .filter(inquiry -> inquiry.getStatus() == InquiryStatus.ANSWERED && inquiry.getAnswer() != null)
                .forEach(inquiry -> docs.add(new CorpusDoc(
                        "ANSWERED_INQUIRY", inquiry.getId(), inquiry.getContent() + "\n답변: " + inquiry.getAnswer())));

        assistClient.clear();
        assistClient.upsert(docs);
        return docs.size();
    }

    private Inquiry getInquiryOrThrow(Long inquiryId) {
        return inquiryRepository.findById(inquiryId).orElseThrow(() -> new BaseException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    // 목록 조회는 호출자가 넘긴 정렬을 무시하고 항상 최신순으로 고정한다.
    // (기존 InquiryRepositoryImpl.sortByCreatedAtDesc 동작을 그대로 옮긴 것)
    private PageRequest sortByCreatedAtDesc(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private InquiryDetailResponse toDetailResponse(Inquiry inquiry, String nickname) {
        return new InquiryDetailResponse(
                inquiry.getId(),
                nickname,
                inquiry.getType().getDescription(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus().getDescription(),
                inquiry.getCreatedAt());
    }

    private InquirySummary toSummary(Inquiry inquiry) {
        return new InquirySummary(
                inquiry.getId(),
                inquiry.getUserId(),
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt());
    }
}
