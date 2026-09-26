package com.back.catchmate.inquiry.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.inquiry.domain.InquiryErrorCode;

public class InquiryNotFoundException extends BusinessException {
    public InquiryNotFoundException() {
        super(InquiryErrorCode.INQUIRY_NOT_FOUND);
    }
}
