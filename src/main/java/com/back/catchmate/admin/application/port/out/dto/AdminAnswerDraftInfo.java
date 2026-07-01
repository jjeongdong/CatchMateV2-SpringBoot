package com.back.catchmate.admin.application.port.out.dto;

import java.util.List;

public record AdminAnswerDraftInfo(
        boolean grounded,
        String draft,
        List<String> sources
) {
}
