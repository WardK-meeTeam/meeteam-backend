package com.wardk.meeteam_backend.web.auth.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenExchangeResponse {
    private String accessToken;
}
