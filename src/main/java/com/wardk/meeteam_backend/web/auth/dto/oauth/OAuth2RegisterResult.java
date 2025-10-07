package com.wardk.meeteam_backend.web.auth.dto.oauth;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class OAuth2RegisterResult {
    private Member member;
    private String accessToken;
    private String refreshToken;
}
