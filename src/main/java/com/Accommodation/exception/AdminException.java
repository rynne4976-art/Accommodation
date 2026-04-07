package com.Accommodation.exception;

import lombok.Getter;

@Getter
public class AdminException extends RuntimeException {

    private final ErrorCode errorCode;

    public AdminException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
