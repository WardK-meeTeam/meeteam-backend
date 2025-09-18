package com.wardk.meeteam_backend.global.auth.dto.register;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {

    private String username;

    public static RegisterResponse responseDto(String username) {
        return RegisterResponse.builder()
                .username(username)
                .build();
    }

}
