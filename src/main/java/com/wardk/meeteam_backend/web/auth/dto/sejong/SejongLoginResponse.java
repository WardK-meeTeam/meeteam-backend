package com.wardk.meeteam_backend.web.auth.dto.sejong;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SejongLoginResponse {

    private String accessToken;
    private boolean isNewMember;

    public static SejongLoginResponse existingMember(String accessToken) {
        return new SejongLoginResponse(accessToken, false);
    }

    public static SejongLoginResponse newMember() {
        return new SejongLoginResponse(null, true);
    }
}