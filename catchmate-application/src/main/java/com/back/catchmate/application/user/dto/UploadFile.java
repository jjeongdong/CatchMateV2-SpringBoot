package com.back.catchmate.application.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UploadFile {
    private String originalFilename;
    private String contentType;
    private InputStream inputStream;
    private long size;
}
