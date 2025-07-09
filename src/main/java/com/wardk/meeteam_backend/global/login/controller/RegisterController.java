package com.wardk.meeteam_backend.global.login.controller;

import com.wardk.meeteam_backend.global.login.dto.register.RegisterRequestDto;
import com.wardk.meeteam_backend.global.login.dto.register.RegisterResponseDto;
import com.wardk.meeteam_backend.global.login.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("/api/register")
    public ApiResponse<RegisterResponseDto> response(@RequestBody RegisterRequestDto request) {



    }
}
