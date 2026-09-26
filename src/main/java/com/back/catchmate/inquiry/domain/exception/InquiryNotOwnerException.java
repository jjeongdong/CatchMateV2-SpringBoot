package com.back.catchmate.inquiry.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.inquiry.domain.InquiryErrorCode;

public class InquiryNotOwnerException extends BusinessException {
    public InquiryNotOwnerException() {
        super(InquiryErrorCode.INQUIRY_NOT_OWNER);
    }
}
