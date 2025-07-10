package com.wardk.meeteam_backend.global.loginRegister.controller;

import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.register.RegisterRequestDto;
import com.wardk.meeteam_backend.global.loginRegister.dto.register.RegisterResponseDto;
import com.wardk.meeteam_backend.global.loginRegister.service.RegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping(value = "/api/register" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<RegisterResponseDto> response(@ModelAttribute @Validated RegisterRequestDto request) throws IOException {
        log.info("회원가입={}",request.getName());


        String name = registerService.register(request);

        return SuccessResponse.onSuccess(RegisterResponseDto.responseDto(name));

    }
}
