package com.wardk.meeteam_backend.global.auth.dto.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

  private String name;
  private Long memberId;

}
