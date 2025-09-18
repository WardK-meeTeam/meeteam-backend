package com.wardk.meeteam_backend.global.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.response.SuccessCode;
import com.wardk.meeteam_backend.global.response.ErrorResponse;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.auth.dto.login.LoginRequest;
import com.wardk.meeteam_backend.global.auth.dto.login.LoginResponse;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;
  private final ObjectMapper objectMapper = new ObjectMapper();


  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

    // 클라이언트 요청에서 username, password 추출
    try {
      // 요청 본문에서 JSON 데이터를 파싱
      LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

      String username = loginRequest.getUsername();
      String password = loginRequest.getPassword();

      // 스프링 시큐리티에서 username과 password를 검증하기 위해서는 token에 담아야 함
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

      // token 검증을 위한 AuthenticationManager로 전달
      return authenticationManager.authenticate(authToken);
    } catch (IOException e) {
      log.error("JSON 파싱 중 오류 발생");
      // ★ 여기서 CustomException 던지면 전역 핸들러가 못 잡음(필터 단계라서)
      //    AuthenticationException 계열로 던져서 아래 unsuccessfulAuthentication로 가게 한다.
      throw new AuthenticationServiceException("INVALID_REQUEST", e);
    }
  }

  //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
    // UserDetails
    CustomSecurityUserDetails customSecurityUserDetails = (CustomSecurityUserDetails) authentication.getPrincipal();

    // AccessToken 발급
    String accessToken = jwtUtil.createAccessToken(customSecurityUserDetails);

    // RefreshToken 발급
    String refreshToken = jwtUtil.createRefreshToken(customSecurityUserDetails);

    // 헤더에 AccessToken 추가
    response.addHeader("Authorization", "Bearer " + accessToken);

    // 2) JS에서 읽을 수 있도록 expose-header 추가
    response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.AUTHORIZATION);



    // 쿠키에 refreshToken 추가
    Cookie cookie = new Cookie("refreshToken", refreshToken);
    cookie.setHttpOnly(true); // HttpOnly 설정
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge((int) (jwtUtil.getRefreshExpirationTime() / 1000)); // 쿠키 maxAge는 초 단위 이므로, 밀리초를 1000으로 나눔
    response.addCookie(cookie);

    // 로그인에 성공하면 유저 정보 반환
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // 반환할 유저 정보
    LoginResponse loginResponse = LoginResponse.builder()
            .name(customSecurityUserDetails.getUsername())
            .memberId(customSecurityUserDetails.getMemberId())
            .build();

    SuccessResponse<LoginResponse> apiResponse = SuccessResponse.of(SuccessCode._LOGIN_SUCCESS, loginResponse);
    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    response.setStatus(HttpServletResponse.SC_OK);
  }


  //로그인 실패시 실행하는 메소드
  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {

    log.error("로그인 실패: {}", failed.getMessage());

    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    // 실패 원인에 따라 코드 분기 (원하면 세분화)
    ErrorCode ec;
    if (failed instanceof AuthenticationServiceException && "INVALID_REQUEST".equals(failed.getMessage())) {
      ec = ErrorCode.INVALID_REQUEST;          // 바디 파싱 등 요청 자체가 잘못된 경우
    } else if (failed instanceof BadCredentialsException) {
      ec = ErrorCode.BAD_CREDENTIALS;             // 아이디/비번 불일치 (원하면 BAD_CREDENTIALS 등 별도 코드)
    } else {
      ec = ErrorCode.BAD_CREDENTIALS;
    }

    ErrorResponse body = ErrorResponse.getResponse(ec.getCode(), ec.getMessage());
    response.getWriter().write(objectMapper.writeValueAsString(body));

  }
}
