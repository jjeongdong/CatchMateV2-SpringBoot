package com.back.catchmate.notice.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notice.dto.response.NoticeCreateResponse;
import com.back.catchmate.notice.dto.response.NoticeDetailResponse;
import com.back.catchmate.notice.dto.response.NoticeResponse;
import com.back.catchmate.notice.dto.response.NoticeSummary;
import com.back.catchmate.notice.entity.Notice;
import com.back.catchmate.notice.repository.NoticeRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
public class NoticeService {
    // 코퍼스 색인용 일괄 조회 상한 (공지는 소규모라 단일 페이지로 충분)
    private static final int MAX_CORPUS_FETCH = 1000;

    private final NoticeRepository noticeRepository;
    private final UserQueryApi userQueryApi;

    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        Notice notice = getNoticeOrThrow(noticeId);
        UserInfo writer = userQueryApi.getInfo(notice.getWriterId());
        return toDetailResponse(notice, writer.nickName());
    }

    public PagedResponse<NoticeResponse> getNoticeList(int page, int size) {
        Page<Notice> noticePage = findNoticePage(PageRequest.of(page, size));

        Map<Long, String> writerNicknameById = fetchWriterNicknames(noticePage.getContent());

        List<NoticeResponse> responses = noticePage.getContent().stream()
                .map(n -> toNoticeResponse(n, writerNicknameById.getOrDefault(n.getWriterId(), "")))
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    // 다른 컨텍스트용 — 단건 요약
    public NoticeSummary getNoticeSummary(Long noticeId) {
        return toSummary(getNoticeOrThrow(noticeId));
    }

    // 다른 컨텍스트용 — 페이징 요약
    public Page<NoticeSummary> getNoticeSummaries(Pageable pageable) {
        return findNoticePage(pageable).map(this::toSummary);
    }

    // 다른 컨텍스트용 — 코퍼스 색인 일괄 조회
    public List<NoticeSummary> getAllNoticeSummaries() {
        return findNoticePage(PageRequest.of(0, MAX_CORPUS_FETCH))
                .map(this::toSummary)
                .getContent();
    }

    @Transactional
    public NoticeCreateResponse createNotice(Long writerId, String title, String content) {
        Notice notice = Notice.createNotice(writerId, title, content);
        Notice savedNotice = noticeRepository.save(notice);
        return new NoticeCreateResponse(savedNotice.getId(), savedNotice.getCreatedAt());
    }

    @Transactional
    public void updateNotice(Long noticeId, String title, String content) {
        Notice notice = getNoticeOrThrow(noticeId);
        notice.updateNotice(title, content);
        noticeRepository.save(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = getNoticeOrThrow(noticeId);
        noticeRepository.delete(notice);
    }

    private Notice getNoticeOrThrow(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(() -> new BaseException(ErrorCode.NOTICE_NOT_FOUND));
    }

    // 목록 조회는 호출자가 넘긴 정렬을 무시하고 항상 최신순으로 고정한다.
    // (기존 NoticeRepositoryImpl.findAll 이 pageable 의 sort 를 덮어쓰던 동작을 그대로 옮긴 것)
    private Page<Notice> findNoticePage(Pageable pageable) {
        PageRequest sortedPageRequest = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return noticeRepository.findAll(sortedPageRequest);
    }

    private NoticeDetailResponse toDetailResponse(Notice notice, String writerNickname) {
        return new NoticeDetailResponse(
                notice.getId(), notice.getTitle(), notice.getContent(), writerNickname, notice.getCreatedAt());
    }

    private NoticeResponse toNoticeResponse(Notice notice, String writerNickname) {
        return new NoticeResponse(notice.getId(), notice.getTitle(), writerNickname, notice.getCreatedAt());
    }

    private NoticeSummary toSummary(Notice notice) {
        return new NoticeSummary(
                notice.getId(), notice.getWriterId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }

    private Map<Long, String> fetchWriterNicknames(List<Notice> notices) {
        List<Long> writerIds =
                notices.stream().map(Notice::getWriterId).distinct().toList();
        if (writerIds.isEmpty()) return Map.of();
        return userQueryApi.getInfos(writerIds).values().stream()
                .collect(Collectors.toMap(UserInfo::userId, UserInfo::nickName));
    }
}
