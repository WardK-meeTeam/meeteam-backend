package com.wardk.meeteam_backend.web.auth.controller;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.auth.cookie.RefreshTokenCookieProvider;
import com.wardk.meeteam_backend.global.auth.service.dto.TokenExchangeResult;
import com.wardk.meeteam_backend.global.auth.service.dto.RegisterMemberCommand;
import com.wardk.meeteam_backend.web.auth.dto.EmailDuplicateResponse;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterResponse;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterResult;
import com.wardk.meeteam_backend.web.auth.dto.oauth.TokenExchangeRequest;
import com.wardk.meeteam_backend.web.auth.dto.oauth.TokenExchangeResponse;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterDescriptionRequest;
import com.wardk.meeteam_backend.global.response.SuccessCode;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterResponse;
import com.wardk.meeteam_backend.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    private final RefreshTokenCookieProvider cookieProvider;

    @Operation(summary = "회원가입", description = "회원 정보를 입력받아 계정을 생성합니다.")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponse> response(
            @RequestPart("request") @Valid RegisterRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("회원가입={}", request.getName());
        return SuccessResponse.onSuccess(authService.register(RegisterMemberCommand.from(request), file));
    }


    @Operation(summary = "OAuth2 회원가입", description = "OAuth2 회원가입 전용 페이지에서 일회용 코드와 회원 정보를 입력받아 계정을 생성 후, 로그인 처리를 합니다.")
    @PostMapping(value = "/register/oauth2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<OAuth2RegisterResponse> oAuth2Register(
        HttpServletResponse response,
        @RequestPart("request") @Valid OAuth2RegisterRequest request,
        @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("OAuth2 회원가입 요청 - name: {}", request.getName());

        OAuth2RegisterResult result = authService.oauth2Register(request, file);

        cookieProvider.addCookie(response, result.getRefreshToken());

        Member member = result.getMember();
        return SuccessResponse.onSuccess(
            new OAuth2RegisterResponse(member.getRealName(), member.getId(), result.getAccessToken())
        );
    }

    @Operation(summary = "OAuth2 토큰 교환", description = "OAuth2 로그인 후 전달받은 일회용 코드를 사용하여 Access Token과 Refresh Token을 교환합니다.")
    @PostMapping("/token/exchange")
    public SuccessResponse<TokenExchangeResponse> exchangeToken(
        HttpServletResponse response,
        @RequestBody @Valid TokenExchangeRequest request
    ) {
        log.info("OAuth2 토큰 교환 요청");

        TokenExchangeResult result = authService.exchangeToken(request.getCode());

        cookieProvider.addCookie(response, result.getRefreshToken());
        log.info("토큰 교환 완료 - Refresh Token 쿠키 설정");

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
        // Authorization 헤더에서 AccessToken 추출
        String accessToken = extractAccessToken(request);

        // 로그아웃 처리 (토큰 블랙리스트 추가)
        authService.logout(accessToken);

        // Refresh Token 쿠키 삭제
        cookieProvider.deleteCookie(response);

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
