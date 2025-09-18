package com.wardk.meeteam_backend.global.auth.controller;

import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.global.auth.dto.login.LoginRequest;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.global.auth.dto.register.RegisterResponse;
import com.wardk.meeteam_backend.global.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     *  요청 형식
     * {
     *   "name": "홍길동",
     *   "age": 27,
     *   "email": "meeteam@naver.com",
     *   "gender": "MALE",
     *   "password": "qwer1234",
     *   "subCategories": [
     *     { "subcategory": "웹서버" },
     *     { "subcategory": "AI" },
     *     { "subcategory": "DBA/빅데이터/DS" }
     *   ],
     *   "skills": [
     *     { "skillName": "Java" },
     *     { "skillName": "Spring Boot" },
     *     { "skillName": "MySQL" }
     *   ]
     * }
     *
     * @param request
     * @param file
     * @return
     */


    @PostMapping(value = "/register" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponse> response(
            @RequestPart("request") RegisterRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("회원가입={}",request.getName());

        String name = authService.register(request, file);

        return SuccessResponse.onSuccess(RegisterResponse.responseDto(name));

    }

    // 자기 소개 등록 API 분리
    @PostMapping("/register/introduce")
    public SuccessResponse<RegisterResponse> registerIntroduce(
            @RequestParam String email, @RequestParam String introduce
    ) {
            log.info("자기소개 등록 이메일={}, 소개={}", email, introduce);

            String name = authService.registerIntroduce(email, introduce);

            return SuccessResponse.onSuccess(RegisterResponse.responseDto(name));
        }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return request.getUsername();
    }

}
