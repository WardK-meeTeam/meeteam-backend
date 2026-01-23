package com.wardk.meeteam_backend.web.auth.controller;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import com.wardk.meeteam_backend.web.auth.dto.EmailDuplicateResponse;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterResult;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterDescriptionRequest;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.auth.dto.login.LoginRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterResponse;
import com.wardk.meeteam_backend.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
@Tag(description = "인증 및 회원 관리 API", name = "AuthController")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 요청 형식
     * {
     * "name": "홍길동",
     * "age": 27,
     * "email": "meeteam@naver.com",
     * "gender": "MALE",
     * "password": "qwer1234",
     * "subCategories": [
     * { "subcategory": "웹서버" },
     * { "subcategory": "AI" },
     * { "subcategory": "DBA/빅데이터/DS" }
     * ],
     * "skills": [
     * { "skillName": "Java" },
     * { "skillName": "Spring Boot" },
     * { "skillName": "MySQL" }
     * ]
     * }
     *
     * @param request
     * @param file
     * @return
     */
    @Operation(summary = "회원가입", description = "회원 정보를 입력받아 계정을 생성합니다.")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponse> response(
            @RequestPart("request") RegisterRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("회원가입={}", request.getName());
        return SuccessResponse.onSuccess(authService.register(request, file));
    }

    @Operation(summary = "OAuth2 회원가입", description = "OAuth2 회원가입 전용 페이지에서 회원 정보를 입력받아 계정을 생성 후, 로그인 처리를 합니다. 헤더에 액세스 토큰을 반환합니다.")
    @PostMapping(value = "/register/oauth2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponse> oAuth2Register(
        HttpServletResponse response,
        @RequestPart("request") OAuth2RegisterRequest request,
        @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("OAuth2 회원가입={}", request.getName());
        OAuth2RegisterResult result = authService.oauth2Register(request, file);
        // ResponseCookie 빌더를 사용하여 SameSite 속성을 명시적으로 설정
        ResponseCookie responseCookie = ResponseCookie.from(JwtUtil.REFRESH_COOKIE_NAME, result.getRefreshToken())
            .httpOnly(true)    // HttpOnly 설정
            .secure(true)      // Secure 설정 (HTTPS 필수)
            .path("/")         // Path 설정
            .domain(".meeteam.alom-sejong.com") // 도메인 설정 (점 포함)
            .sameSite("None")                   // 서브도메인 간 공유를 허용
            .maxAge(jwtUtil.getRefreshExpirationTime() / 1000) // MaxAge 설정
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
        log.info("Refresh Token 쿠키 설정 완료");

        // 로그인에 성공하면 유저 정보 반환
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Member member = result.getMember();
        return SuccessResponse.onSuccess(new RegisterResponse(member.getRealName(), member.getId()));
    }

    @Operation(summary = "자기소개 등록", description = "회원가입 후 자기소개.")
    @PostMapping(value = "/register/{memberId}")
    public SuccessResponse<RegisterResponse> register(
            @RequestBody RegisterDescriptionRequest descriptionRequest,
            @PathVariable Long memberId
    ) {
        RegisterResponse registerResponse = authService.registDesciption(memberId, descriptionRequest);
        return SuccessResponse.onSuccess(registerResponse);
    }
    /*@PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponse> response(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws JsonProcessingException {
        RegisterRequest request = new ObjectMapper().readValue(requestJson, RegisterRequest.class);
        log.info("회원가입={}", request.getName());

        String name = authService.register(request, file);

        return SuccessResponse.onSuccess(RegisterResponse.responseDto(name));

    }*/



    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return request.getUsername();
    }

    @Operation(summary = "이메일 중복 체크", description = "입력한 이메일이 이미 존재하는지 확인합니다.")
    @PostMapping("/email")
    public SuccessResponse<EmailDuplicateResponse> responseEmil(
            @RequestParam String email
    ) {
        return SuccessResponse.onSuccess(authService.checkDuplicateEmail(email));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public SuccessResponse<String> refreshToken(HttpServletRequest request) {
        return SuccessResponse.onSuccess(authService.refreshAccessToken(request));
    }

    @Operation(summary = "로그아웃", description = "로그아웃을 수행합니다. AccessToken을 블랙리스트에 등록하고, Refresh Token 쿠키를 삭제합니다. 클라이언트는 Access Token을 직접 삭제해야 합니다.")
    @PostMapping("/logout")
    public SuccessResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        // Authorization 헤더에서 AccessToken 추출
        String accessToken = extractAccessToken(request);

        // 로그아웃 처리 (토큰 블랙리스트 추가 + 쿠키 삭제)
        ResponseCookie expiredCookie = authService.logout(accessToken);
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

        log.info("로그아웃 완료 - AccessToken 블랙리스트 등록 및 Refresh Token 쿠키 삭제");
        return SuccessResponse.onSuccess("로그아웃이 완료되었습니다.");
    }

    /**
     * Authorization 헤더에서 AccessToken 추출
     */
    private String extractAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

}
