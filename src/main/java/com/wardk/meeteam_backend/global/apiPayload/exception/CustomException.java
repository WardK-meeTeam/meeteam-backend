package com.wardk.meeteam_backend.global.apiPayload.exception;

import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;


    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
