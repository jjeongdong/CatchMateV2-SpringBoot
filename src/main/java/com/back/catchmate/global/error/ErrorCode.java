package com.back.catchmate.global.error;

public interface ErrorCode {
    String name();

    ErrorType type();

    String message();
}
