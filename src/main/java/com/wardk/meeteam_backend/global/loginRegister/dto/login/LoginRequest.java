package com.wardk.meeteam_backend.global.loginRegister.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;


@Getter
@JsonPropertyOrder({ "email", "password" })
public class LoginRequest {

  @JsonProperty("email")
  private String username;

  private String password;
}
