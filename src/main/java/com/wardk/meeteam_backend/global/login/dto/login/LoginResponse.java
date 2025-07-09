package com.wardk.meeteam_backend.global.login.dto.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
  private String username;
  private String name;
}
