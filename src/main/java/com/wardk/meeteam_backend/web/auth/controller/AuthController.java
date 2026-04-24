package com.wardk.meeteam_backend.web.auth.controller;

import com.wardk.meeteam_backend.global.auth.cookie.AccessTokenCookieProvider;
import com.wardk.meeteam_backend.global.auth.cookie.RefreshTokenCookieProvider;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import com.wardk.meeteam_backend.global.auth.service.AuthService;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuth2RegisterCommand;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuth2RegisterResult;
import com.wardk.meeteam_backend.global.auth.service.dto.RegisterMemberCommand;
import com.wardk.meeteam_backend.global.auth.service.dto.SejongLoginCommand;
import com.wardk.meeteam_backend.global.auth.service.dto.SejongLoginResult;
import com.wardk.meeteam_backend.global.auth.service.dto.SejongRegisterCommand;
import com.wardk.meeteam_backend.global.auth.service.dto.TokenExchangeResult;
import com.wardk.meeteam_backend.web.auth.dto.sejong.SejongLoginRequest;
import com.wardk.meeteam_backend.web.auth.dto.sejong.SejongLoginResponse;
import com.wardk.meeteam_backend.web.auth.dto.sejong.SejongRegisterRequest;
import com.wardk.meeteam_backend.global.response.SuccessCode;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.auth.dto.EmailDuplicateResponse;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterResponse;
import com.wardk.meeteam_backend.web.auth.dto.oauth.TokenExchangeRequest;
import com.wardk.meeteam_backend.web.auth.dto.oauth.TokenExchangeResponse;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterDescriptionRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/auth")
@Tag(description = "인증 및 회원 관리 API", name = "AuthController")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final AccessTokenCookieProvider accessTokenCookieProvider;



    @Operation(summary = "회원가입", description = "회원 정보를 입력받아 계정을 생성합니다. 직군/직무/기술스택 정보는 GET /api/jobs/options를 먼저 호출하여 조회하고, 선택한 직군(JobField)에 해당하는 기술스택만 전송해야 합니다.")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponse> register(
            @Parameter(description = "회원가입 정보", required = true)
            @RequestPart("request") @Valid RegisterRequest request,
            @Parameter(description = "프로필 이미지", required = false)
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("회원가입={}", request.getName());
        return SuccessResponse.onSuccess(authService.register(RegisterMemberCommand.from(request), file));
    }



    @Operation(summary = "세종대 포털 로그인", description = "세종대 포털 학번/비밀번호로 로그인합니다. 기존 회원이면 토큰 발급, 신규 회원이면 isNewMember=true와 회원가입용 코드 반환.")
    @PostMapping("/login/sejong")
    public SuccessResponse<SejongLoginResponse> sejongLogin(
            HttpServletResponse response,
            @RequestBody @Valid SejongLoginRequest request
    ) {
        log.info("세종대 포털 로그인 요청 - studentId: {}", request.getStudentId());

        SejongLoginResult result = authService.sejongLogin(
                new SejongLoginCommand(request.getStudentId(), request.getPassword())
        );

        if (result.isNewMember()) {
            // 신규 회원 - 회원가입용 코드 반환
            log.info("세종대 포털 인증 성공 - 신규 회원, 회원가입 필요");
            return SuccessResponse.onSuccess(SejongLoginResponse.newMember(result.getCode()));
        }

        // 기존 회원 - 토큰 발급
        accessTokenCookieProvider.addCookie(response, result.getAccessToken());
        refreshTokenCookieProvider.addCookie(response, result.getRefreshToken());
        log.info("세종대 포털 로그인 완료 - 기존 회원");

        return SuccessResponse.onSuccess(SejongLoginResponse.existingMember());
    }

    @Operation(summary = "세종대 회원가입", description = "세종대 포털 로그인 후 발급받은 코드와 온보딩 정보로 회원가입합니다.")
    @PostMapping(value = "/register/sejong", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<Void> sejongRegister(
            HttpServletResponse response,
            @Parameter(description = "세종대 회원가입 정보", required = true)
            @RequestPart("request") @Valid SejongRegisterRequest request,
            @Parameter(description = "프로필 이미지", required = false)
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("세종대 회원가입 요청 - name: {}", request.getName());
        TokenExchangeResult result = authService.sejongRegister(SejongRegisterCommand.from(request), file);
        accessTokenCookieProvider.addCookie(response, result.getAccessToken());
        refreshTokenCookieProvider.addCookie(response, result.getRefreshToken());
        log.info("세종대 회원가입 완료");
        return SuccessResponse.onSuccess(null);
    }

    @Operation(summary = "OAuth2 회원가입", description = "OAuth2 회원가입 전용 페이지에서 일회용 코드와 회원 정보를 입력받아 계정을 생성 후, 로그인 처리를 합니다. 직군/직무/기술스택 정보는 GET /api/jobs/options를 먼저 호출하여 조회하고, 선택한 직군(JobField)에 해당하는 기술스택만 전송해야 합니다.")
    @PostMapping(value = "/register/oauth2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<OAuth2RegisterResponse> oAuth2Register(
        HttpServletResponse response,
        @Parameter(description = "OAuth2 회원가입 정보", required = true)
        @RequestPart("request") @Valid OAuth2RegisterRequest request,
        @Parameter(description = "프로필 이미지", required = false)
        @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("OAuth2 회원가입 요청 - name: {}", request.getName());
        OAuth2RegisterResult result = authService.oauth2Register(OAuth2RegisterCommand.from(request), file);
        accessTokenCookieProvider.addCookie(response, result.accessToken());
        refreshTokenCookieProvider.addCookie(response, result.refreshToken());
        return SuccessResponse.onSuccess(OAuth2RegisterResponse.from(result));
    }



    @Operation(summary = "OAuth2 토큰 교환", description = "OAuth2 로그인 후 전달받은 일회용 코드를 사용하여 Access Token과 Refresh Token을 교환합니다.")
    @PostMapping("/token/exchange")
    public SuccessResponse<TokenExchangeResponse> exchangeToken(
        HttpServletResponse response,
        @RequestBody @Valid TokenExchangeRequest request
    ) {
        log.info("OAuth2 토큰 교환 요청");

        TokenExchangeResult result = authService.exchangeToken(request.getCode());

        accessTokenCookieProvider.addCookie(response, result.getAccessToken());
        refreshTokenCookieProvider.addCookie(response, result.getRefreshToken());
        log.info("토큰 교환 완료 - Access Token 및 Refresh Token 쿠키 설정");

        return SuccessResponse.of(SuccessCode._TOKEN_EXCHANGE_SUCCESS, new TokenExchangeResponse(result.getAccessToken()));
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

    @Operation(summary = "로그아웃", description = "로그아웃을 수행합니다. AccessToken을 블랙리스트에 등록하고, OAuth 사용자의 경우 OAuth 제공자(Google/GitHub)의 토큰도 철회합니다. Refresh Token 쿠키를 삭제합니다. 클라이언트는 Access Token을 직접 삭제해야 합니다.")
    @PostMapping("/logout")
    public SuccessResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        // Authorization 헤더 또는 쿠키에서 AccessToken 추출
        String accessToken = extractAccessToken(request);

        // 로그아웃 처리 (토큰 블랙리스트 추가)
        authService.logout(accessToken);

        // Access Token 쿠키 삭제
        accessTokenCookieProvider.deleteCookie(response);
        // Refresh Token 쿠키 삭제
        refreshTokenCookieProvider.deleteCookie(response);

        log.info("로그아웃 완료 - AccessToken 블랙리스트 등록 및 토큰 쿠키 삭제");
        return SuccessResponse.onSuccess("로그아웃이 완료되었습니다.");
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 수행합니다. 계정이 비활성화되며 로그아웃 처리됩니다.")
    @DeleteMapping("/withdraw")
    public SuccessResponse<String> withdraw(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        Long memberId = userDetails.getMemberId();
        log.info("회원 탈퇴 요청 - memberId: {}", memberId);

        // 회원 탈퇴 처리 (소프트 삭제)
        authService.withdraw(memberId);

        // 토큰 블랙리스트 추가 및 OAuth 토큰 철회
        String accessToken = extractAccessToken(request);
        authService.logout(accessToken);

        // 쿠키 삭제
        accessTokenCookieProvider.deleteCookie(response);
        refreshTokenCookieProvider.deleteCookie(response);

        log.info("회원 탈퇴 완료 - memberId: {}", memberId);
        return SuccessResponse.onSuccess("회원 탈퇴가 완료되었습니다.");
    }

    /**
     * Request에서 AccessToken 추출
     * 우선순위: Authorization 헤더 > 쿠키
     */
    private String extractAccessToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 먼저 확인
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        // 2. 쿠키에서 확인
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JwtUtil.ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

}
