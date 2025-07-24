package com.wardk.meeteam_backend.global.apiPayload.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // GLOBAL

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버에 문제가 발생했습니다."),

    INVALID_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400", "잘못된 요청입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON403","접근이 거부되었습니다"),


    // LOGIN
    DUPLICATE_MEMBER(HttpStatus.BAD_REQUEST,"MEMBER400", "이미 존재하는 회원입니다."),
    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST,"MEMBER400" , "다시 로그인하세요"),

    // OAUTH2
    OAUTH2_ATTRIBUTES_EMPTY(HttpStatus.BAD_REQUEST, "OAUTH400", "OAuth2 사용자 정보가 비어있습니다."),
    OAUTH2_PROVIDER_ID_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH401", "OAuth2 제공자 ID를 찾을 수 없습니다."),
    OAUTH2_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH402", "OAuth2 이메일 정보를 찾을 수 없습니다."),
    OAUTH2_NAME_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH403", "OAuth2 사용자 이름 추출에 실패했습니다.");

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private final HttpStatus status;
    private final String code;
    private final String message;
}
