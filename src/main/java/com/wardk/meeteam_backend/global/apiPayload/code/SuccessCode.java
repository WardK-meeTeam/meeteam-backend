package com.wardk.meeteam_backend.global.apiPayload.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessCode {



    //가장 일반적인 응답

    _OK(HttpStatus.OK,"COMMON200", "요청에 성공했습니다."),
    _LOGIN_SUCCESS(HttpStatus.OK, "LOGIN200", "로그인에 성공했습니다")
    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;


    SuccessCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
