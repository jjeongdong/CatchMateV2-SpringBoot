package com.back.catchmate.global.infrastructure.upload;

import java.io.InputStream;

public record UploadFile(String originalFilename, String contentType, InputStream inputStream, long size) {}
