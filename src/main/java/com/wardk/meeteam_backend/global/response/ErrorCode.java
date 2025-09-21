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

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON403", "접근이 거부되었습니다"),


    // LOGIN
    DUPLICATE_MEMBER(HttpStatus.BAD_REQUEST, "MEMBER400", "이미 존재하는 회원입니다."),
    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST, "MEMBER400", "다시 로그인하세요"),

    // OAUTH2
    OAUTH2_ATTRIBUTES_EMPTY(HttpStatus.BAD_REQUEST, "OAUTH400", "OAuth2 사용자 정보가 비어있습니다."),
    OAUTH2_PROVIDER_ID_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH401", "OAuth2 제공자 ID를 찾을 수 없습니다."),
    OAUTH2_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH402", "OAuth2 이메일 정보를 찾을 수 없습니다."),
    OAUTH2_NAME_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH403", "OAuth2 사용자 이름 추출에 실패했습니다."),

    //이미지 첨부
    FILE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST, "FILE401", "파일 저장에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE402", "파일 삭제에 실패했습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "FILE403", "빈 파일은 업로드할 수 없습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "FILE404", "허용되지 않는 파일 확장자입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE405", "파일 크기가 허용된 크기를 초과했습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404", "회원을 찾을 수 없습니다."),

    // PROJECT
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404", "프로젝트를 찾을 수 없습니다."),
    INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, "PROJECT400", "종료일은 시작일 이후여야 합니다."),

    // CHAT
    CHAT_THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404", "해당 쓰레드를 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM404", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "CHAT_ROOM400", "이미 존재하는 채팅방입니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_ROOM403", "채팅방 접근 권한이 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MESSAGE404", "메시지를 찾을 수 없습니다."),
    MESSAGE_EDIT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "MESSAGE403", "메시지 수정 권한이 없습니다."),
    MESSAGE_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "MESSAGE403", "메시지 삭제 권한이 없습니다."),
    MESSAGE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "MESSAGE400", "이미 삭제된 메시지입니다."),
    CANNOT_CHAT_WITH_YOURSELF(HttpStatus.BAD_REQUEST, "CHAT400", "자기 자신과는 채팅할 수 없습니다."),
    PROJECT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT401", "프로젝트 ID가 필요합니다."),

    NOT_THREAD_MEMBER(HttpStatus.FORBIDDEN, "CHAT403", "해당 쓰레드 멤버가 아닙니다."),

    // PROJECT MEMBER
    PROJECT_MEMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROJECT_MEMBER400", "이미 프로젝트에 참여 중입니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_MEMBER404", "프로젝트 멤버가 존재하지 않습니다."),
    PROJECT_MEMBER_FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT_MEMBER403", "해당 프로젝트 멤버 관리 권한이 없습니다."),
    CREATOR_TRANSFER_SELF_DENIED(HttpStatus.BAD_REQUEST, "PROJECT_MEMBER400", "프로젝트 생성자와 변경 대상이 동일합니다."), // 프로젝트 생성자와 동일한 멤버로 변경하려는 경우
    CREATOR_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "PROJECT_MEMBER403", "프로젝트 생성자는 탈퇴할 수 없습니다."), // 프로젝트 생성자가 탈퇴하려는 경우
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITMENT404", "프로젝트 모집 정보를 찾을 수 없습니다."),
    RECRUITMENT_FULL(HttpStatus.BAD_REQUEST, "RECRUITMENT400", "해당 모집은 마감되었습니다."),

    // PROJECT APPLICATION
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_APPLICATION404", "프로젝트 지원이 존재하지 않습니다."),
    APPLICATION_ALREADY_DECIDED(HttpStatus.BAD_REQUEST, "PROJECT_APPLICATION400", "이미 처리된 지원입니다."),
    PROJECT_APPLICATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROJECT_APPLICATION400", "이미 신청한 프로젝트입니다."),

    // MEMBER SKILL
    SKILL_NOT_FOUND(HttpStatus.NOT_FOUND, "SKILL404", "해당 기술스택이 존재하지 않습니다."),
    SUBCATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBCATEGORY404", "해당 직무는 존재하지 않습니다."),


    //NOTIFICATION
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION404", "알림을 찾을 수 없습니다."),
    ACTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTOR404", "행위자(신청자)를 찾을 수 없습니다."),
    INVALID_EVENT_ID(HttpStatus.BAD_REQUEST, "NOTIFICATION_INVALID_EVENT_ID", "이벤트 ID 형식이 올바르지 않습니다."),
    NO_MATCHING_TYPE(HttpStatus.BAD_REQUEST, "NOTIFICATION_NO_MATCHINGTYPE", "맞는 알림 타입정보가 없습니다."),


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


    FAILED_TO_CREATE_APP_JWT(HttpStatus.BAD_REQUEST, "GITHUB400", "GitHub App JWT 생성에 실패했습니다."),
    GITHUB_APP_NOT_INSTALLED(HttpStatus.BAD_REQUEST, "GITHUB401", "해당 레포에 GitHub App이 설치되지 않았습니다."),
    GITHUB_API_ERROR(HttpStatus.BAD_GATEWAY, "GITHUB502", "GitHub API 요청 중 오류가 발생했습니다."),

    // 메인페이지 관련 에러 코드
    MAIN_PAGE_INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "MAIN_PAGE401", "잘못된 페이징 정보입니다."),
    MAIN_PAGE_CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "MAIN_PAGE402", "대분류 정보가 필요합니다."),
    MAIN_PAGE_SORT_PARAMETER_INVALID(HttpStatus.BAD_REQUEST, "MAIN_PAGE403", "정렬 기준이 올바르지 않습니다."),

    
    /* AI 코드 리뷰 관련 에러 코드 */
    // 리소스 관련
    PR_REVIEW_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "CODE_REVIEW404", "해당 코드 리뷰 작업을 찾을 수 없습니다."),
    PR_REVIEW_FINDING_NOT_FOUND(HttpStatus.NOT_FOUND, "CODE_REVIEW404", "해당 코드 리뷰 발견 항목을 찾을 수 없습니다."),
    PR_HUNK_SHARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CODE_REVIEW404", "해당 코드 변경 샤드를 찾을 수 없습니다."),
    
    // 상태 관련
    PR_REVIEW_JOB_ALREADY_RUNNING(HttpStatus.BAD_REQUEST, "CODE_REVIEW400", "이미 실행 중인 코드 리뷰 작업입니다."),
    PR_REVIEW_JOB_INVALID_STATE(HttpStatus.BAD_REQUEST, "CODE_REVIEW400", "유효하지 않은 코드 리뷰 작업 상태입니다."),
    PR_FINDING_INVALID_STATUS(HttpStatus.BAD_REQUEST, "CODE_REVIEW400", "유효하지 않은 발견 항목 상태입니다."),
    
    // 서비스 관련
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI503", "AI 서비스를 일시적으로 사용할 수 없습니다."),
    AI_REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "AI408", "AI 서비스 요청 시간이 초과되었습니다."),
    AI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI429", "AI 서비스 할당량을 초과했습니다."),
    AI_MODEL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI500", "AI 모델 처리 중 오류가 발생했습니다."),
    
    // 크기 형식 제한 관련
    PR_REVIEW_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "CODE_REVIEW413", "코드 변경 사항이 너무 큽니다."),
    PR_REVIEW_UNSUPPORTED_FILE(HttpStatus.BAD_REQUEST, "CODE_REVIEW400", "지원하지 않는 파일 형식입니다."),
    PR_REVIEW_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "CODE_REVIEW429", "코드 리뷰 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    
    // 패치 관련
    PATCH_APPLY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_REVIEW500", "패치 적용에 실패했습니다."),
    PATCH_GENERATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_REVIEW500", "패치 생성에 실패했습니다."),
    
    LLM_CONFIG_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI500", "LLM 설정 오류가 발생했습니다."),
    LLM_CONTEXT_OVERFLOW(HttpStatus.PAYLOAD_TOO_LARGE, "AI413", "LLM 컨텍스트 크기가 최대 허용치를 초과했습니다."),
    
    // 일반 처리 오류
    CODE_REVIEW_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_REVIEW500", "코드 리뷰 생성에 실패했습니다."),
    CODE_REVIEW_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_REVIEW500", "코드 리뷰 처리 중 오류가 발생했습니다."),
    CODE_REVIEW_CHAT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_REVIEW500", "코드 리뷰 채팅 처리 중 오류가 발생했습니다."),

    UNSUPPORTED_TASK_TYPE(HttpStatus.BAD_REQUEST, "AI400", "지원하지 않는 태스크 유형입니다."),

    //DB
    DB_LIKES_DUPLICATE(HttpStatus.MULTI_STATUS, "DB_CONSTRAINT", "DB 무결성 위반"), DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,"EMAIL_400","이미 존재하는 이메일 입니다." );


    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private final HttpStatus status;
    private final String code;
    private final String message;
}
