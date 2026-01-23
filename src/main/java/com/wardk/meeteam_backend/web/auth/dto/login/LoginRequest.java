package com.wardk.meeteam_backend.web.auth.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({ "email", "password" })
public class LoginRequest {

  @JsonProperty("email")
  private String username;

  private String password;
}
