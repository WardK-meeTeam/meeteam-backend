package com.wardk.meeteam_backend.web.auth.dto.oauth;

import com.wardk.meeteam_backend.global.auth.service.dto.OAuth2RegisterResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuth2RegisterResponse {
    private String username;
    private Long memberId;
    private String accessToken;

    public static OAuth2RegisterResponse from(OAuth2RegisterResult result) {
        return new OAuth2RegisterResponse(
                result.username(),
                result.memberId(),
                result.accessToken()
        );
    }
}