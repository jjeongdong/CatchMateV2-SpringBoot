package com.back.catchmate.user.application.dto.command;

import java.io.InputStream;

public record UploadFile(
        String originalFilename,
        String contentType,
        InputStream inputStream,
        long size
) {
}
