package com.back.catchmate.inquiry.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.inquiry.domain.InquiryErrorCode;

public class InquiryAlreadyAnsweredException extends BusinessException {
    public InquiryAlreadyAnsweredException() {
        super(InquiryErrorCode.INQUIRY_ALREADY_ANSWERED);
    }
}
