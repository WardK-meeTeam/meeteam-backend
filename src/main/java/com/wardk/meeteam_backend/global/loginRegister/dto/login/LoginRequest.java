package com.wardk.meeteam_backend.global.loginRegister.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LoginRequest {
  @JsonProperty("email")
  private String username;

  private String password;
}
