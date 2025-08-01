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
    OAUTH2_NAME_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH403", "OAuth2 사용자 이름 추출에 실패했습니다."),

    //이미지 첨부
    FILE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST,"FILE401", "파일 저장에 실패했습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404", "회원을 찾을 수 없습니다."),

    // PROJECT
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404", "프로젝트를 찾을 수 없습니다."),

    // PROJECT MEMBER
    PROJECT_MEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROJECT_MEMBER400", "이미 프로젝트에 참여 중입니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_MEMBER404", "프로젝트 멤버가 존재하지 않습니다."),
    PROJECT_MEMBER_FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT_MEMBER403", "해당 프로젝트 멤버 관리 권한이 없습니다."),
    CREATOR_TRANSFER_SELF_DENIED(HttpStatus.BAD_REQUEST, "PROJECT_MEMBER400", "프로젝트 생성자와 변경 대상이 동일합니다."), // 프로젝트 생성자와 동일한 멤버로 변경하려는 경우
    CREATOR_WITHDRAW_FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT_MEMBER403", "프로젝트 생성자는 탈퇴할 수 없습니다."), // 프로젝트 생성자가 탈퇴하려는 경우

    // PROJECT APPLICATION
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_APPLICATION404", "프로젝트 지원이 존재하지 않습니다."),
    APPLICATION_ALREADY_DECIDED(HttpStatus.BAD_REQUEST, "PROJECT_APPLICATION400", "이미 처리된 지원입니다."),
    PROJECT_APPLICATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROJECT_APPLICATION400", "이미 신청한 프로젝트입니다.");

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private final HttpStatus status;
    private final String code;
    private final String message;
}
