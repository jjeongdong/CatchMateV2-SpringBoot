package com.back.catchmate.report.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.report.domain.ReportErrorCode;

public class ReportSelfNotAllowedException extends BusinessException {
    public ReportSelfNotAllowedException() {
        super(ReportErrorCode.REPORT_SELF_NOT_ALLOWED);
    }
}
