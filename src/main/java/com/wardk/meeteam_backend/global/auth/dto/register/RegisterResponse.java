package com.wardk.meeteam_backend.global.auth.dto.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RegisterResponse {

    private String username;
    private Long memberId;


}
