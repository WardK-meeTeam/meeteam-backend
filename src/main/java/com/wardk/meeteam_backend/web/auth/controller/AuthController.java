package com.wardk.meeteam_backend.web.auth.controller;


import com.wardk.meeteam_backend.web.auth.dto.EmailDuplicateResponse;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterDescriptionRequest;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.auth.dto.login.LoginRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterResponse;
import com.wardk.meeteam_backend.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

        RegisterResponse register = authService.register(request, file);

        return SuccessResponse.onSuccess(register);
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

}
