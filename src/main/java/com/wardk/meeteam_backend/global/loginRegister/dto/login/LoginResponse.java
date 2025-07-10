package com.wardk.meeteam_backend.global.loginRegister.dto.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
  private String username;
  private String name;
}
