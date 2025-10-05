package com.wardk.meeteam_backend.global.auth.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class SignupTokenInfo {
    private final String email;
    private final String providerId;
    private final String provider;
}