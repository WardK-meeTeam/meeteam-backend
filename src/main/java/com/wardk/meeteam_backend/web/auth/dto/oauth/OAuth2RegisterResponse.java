package com.wardk.meeteam_backend.web.auth.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuth2RegisterResponse {
    private String username;
    private Long memberId;
    private String accessToken;
}
