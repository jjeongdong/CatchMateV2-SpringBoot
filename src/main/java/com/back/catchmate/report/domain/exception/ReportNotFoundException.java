package com.back.catchmate.report.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.report.domain.ReportErrorCode;

public class ReportNotFoundException extends BusinessException {
    public ReportNotFoundException() {
        super(ReportErrorCode.REPORT_NOT_FOUND);
    }
}
