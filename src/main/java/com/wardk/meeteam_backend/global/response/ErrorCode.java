package com.wardk.meeteam_backend.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // GLOBAL

    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),

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

    // CHAT
    CHAT_THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404", "해당 쓰레드를 찾을 수 없습니다."),

    NOT_THREAD_MEMBER(HttpStatus.FORBIDDEN, "CHAT403", "해당 쓰레드 멤버가 아닙니다."),

    // PROJECT MEMBER
    PROJECT_MEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROJECT_MEMBER400", "이미 프로젝트에 참여 중입니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_MEMBER404", "프로젝트 멤버가 존재하지 않습니다."),
    PROJECT_MEMBER_FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT_MEMBER403", "해당 프로젝트 멤버 관리 권한이 없습니다."),
    CREATOR_TRANSFER_SELF_DENIED(HttpStatus.BAD_REQUEST, "PROJECT_MEMBER400", "프로젝트 생성자와 변경 대상이 동일합니다."), // 프로젝트 생성자와 동일한 멤버로 변경하려는 경우
    CREATOR_WITHDRAW_FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT_MEMBER403", "프로젝트 생성자는 탈퇴할 수 없습니다."), // 프로젝트 생성자가 탈퇴하려는 경우
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITMENT404", "프로젝트 모집 정보를 찾을 수 없습니다."),
    RECRUITMENT_FULL(HttpStatus.BAD_REQUEST, "RECRUITMENT400", "해당 모집은 마감되었습니다."),

    // PROJECT APPLICATION
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_APPLICATION404", "프로젝트 지원이 존재하지 않습니다."),
    APPLICATION_ALREADY_DECIDED(HttpStatus.BAD_REQUEST, "PROJECT_APPLICATION400", "이미 처리된 지원입니다."),
    PROJECT_APPLICATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROJECT_APPLICATION400", "이미 신청한 프로젝트입니다."),

    // MEMBER SKILL
    SKILL_NOT_FOUND(HttpStatus.NOT_FOUND,"SKILL404", "해당 기술스택이 존재하지 않습니다."),
    SUBCATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBCATEGORY404","해당 직무는 존재하지 않습니다." ),


    //NOTIFICATION
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION404", "알림을 찾을 수 없습니다."),
    ACTOR_NOT_FOUND(HttpStatus.NOT_FOUND,"ACTOR404", "행위자(신청자)를 찾을 수 없습니다."),
    INVALID_EVENT_ID(HttpStatus.BAD_REQUEST,"NOTIFICATION_INVALID_EVENT_ID" ,"이벤트 ID 형식이 올바르지 않습니다."),
    NO_MATCHING_TYPE(HttpStatus.BAD_REQUEST,"NOTIFICATION_NO_MATCHINGTYPE","맞는 알림 타입정보가 없습니다." ),


    // Webhook 관련 에러 코드
    WEBHOOK_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "WH001", "웹훅 서명이 유효하지 않습니다"),
    WEBHOOK_SIGNATURE_REQUIRED(HttpStatus.BAD_REQUEST, "WH002", "웹훅 서명이 필요합니다"),
    WEBHOOK_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WH003", "웹훅 처리 중 오류가 발생했습니다"),
    WEBHOOK_DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "WH004", "웹훅 기록을 찾을 수 없습니다"),

    // PR
    PR_NOT_FOUND(HttpStatus.NOT_FOUND, "PR404", "해당 Pull Request를 찾을 수 없습니다."),
    PROJECT_REPO_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_REPO404", "해당 프로젝트 저장소를 찾을 수 없습니다."),
    PROJECT_REPO_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROJECT_REPO400", "이미 등록된 프로젝트 저장소입니다."),
    INVALID_REPO_URL(HttpStatus.BAD_REQUEST, "PROJECT_REPO400", "유효하지 않은 저장소 URL입니다."),
    FAILED_TO_PARSE_REPO_URL(HttpStatus.BAD_REQUEST, "PROJECT_REPO400", "저장소 URL 파싱에 실패했습니다."),

    // 메인페이지 관련 에러 코드
    MAIN_PAGE_INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "MAIN_PAGE401", "잘못된 페이징 정보입니다."),
    MAIN_PAGE_CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "MAIN_PAGE402", "대분류 정보가 필요합니다."),
    MAIN_PAGE_SORT_PARAMETER_INVALID(HttpStatus.BAD_REQUEST, "MAIN_PAGE403", "정렬 기준이 올바르지 않습니다.");

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private final HttpStatus status;
    private final String code;
    private final String message;
}
