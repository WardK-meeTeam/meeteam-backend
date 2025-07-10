package com.wardk.meeteam_backend.global.loginRegister.dto.register;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponseDto {

    private String username;

    public static RegisterResponseDto responseDto(String username) {
        return RegisterResponseDto.builder()
                .username(username)
                .build();
    }

}
