package com.wardk.meeteam_backend.global.loginRegister.controller;

import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.register.RegisterRequestDto;
import com.wardk.meeteam_backend.global.loginRegister.dto.register.RegisterResponseDto;
import com.wardk.meeteam_backend.global.loginRegister.service.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RegisterController {

    private final RegisterService registerService;


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


    @PostMapping(value = "/api/register" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponseDto> response(
            @RequestPart("request")  RegisterRequestDto request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("회원가입={}",request.getName());

        String name = registerService.register(request, file);

        return SuccessResponse.onSuccess(RegisterResponseDto.responseDto(name));

    }
}
