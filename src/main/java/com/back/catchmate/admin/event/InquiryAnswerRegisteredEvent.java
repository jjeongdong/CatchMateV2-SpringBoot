package com.back.catchmate.admin.event;

public record InquiryAnswerRegisteredEvent(Long inquiryId, Long inquiryAuthorId) {
    public static InquiryAnswerRegisteredEvent of(Long inquiryId, Long inquiryAuthorId) {
        return new InquiryAnswerRegisteredEvent(inquiryId, inquiryAuthorId);
    }
}
