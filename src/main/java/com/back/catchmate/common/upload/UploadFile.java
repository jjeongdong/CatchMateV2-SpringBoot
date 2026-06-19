package com.back.catchmate.common.upload;

import java.io.InputStream;

public record UploadFile(
        String originalFilename,
        String contentType,
        InputStream inputStream,
        long size
) {
}
