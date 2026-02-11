package com.wardk.meeteam_backend.acceptance.cucumber.steps.constant;

/**
 * 테스트용 상수 관리
 */
public class TestConstants {

    private TestConstants() {} // 인스턴스화 방지

    // === 오류 메시지 ===
    public static final String 오류_이메일중복 = "이미 존재하는 이메일입니다";
    public static final String 오류_비밀번호길이 = "비밀번호는 최소 8자 이상이어야 합니다";
    public static final String 오류_이메일형식 = "올바른 이메일 형식이 아닙니다";
    public static final String 오류_필수항목누락 = "필수 항목을 모두 입력해주세요";
    public static final String 오류_로그인실패 = "이메일 또는 비밀번호가 올바르지 않습니다";
    public static final String 오류_리프레시토큰 = "유효하지 않은 리프레시 토큰입니다";
    public static final String 오류_세션만료 = "세션이 만료되었습니다. 다시 로그인해주세요";
    public static final String 오류_로그인필요 = "로그인이 필요합니다";

    // === 성공 메시지 ===
    public static final String 성공_이메일사용가능 = "사용 가능한 이메일입니다";

    // === 테스트 데이터 ===
    public static final String 기본_이메일 = "test@example.com";
    public static final String 기본_비밀번호 = "password123";
    public static final String 기본_이름 = "테스트회원";

    // === HTTP 상태 ===
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;
}