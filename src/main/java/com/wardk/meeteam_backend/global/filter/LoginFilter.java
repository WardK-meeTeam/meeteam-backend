package com.wardk.meeteam_backend.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.code.SuccessCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.apiPayload.response.ErrorResponse;
import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomUserDetails;
import com.wardk.meeteam_backend.global.loginRegister.dto.login.LoginRequest;
import com.wardk.meeteam_backend.global.loginRegister.dto.login.LoginResponse;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
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
      throw new CustomException(ErrorCode.INVALID_REQUEST);
    }
  }

  //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
    // UserDetails
    CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

    // AccessToken 발급
    String accessToken = jwtUtil.createAccessToken(customUserDetails);

    // RefreshToken 발급
    String refreshToken = jwtUtil.createRefreshToken(customUserDetails);

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
            .username(customUserDetails.getUsername())
            .build();

    SuccessResponse<LoginResponse> apiResponse = SuccessResponse.of(SuccessCode._LOGIN_SUCCESS, null);
    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    response.setStatus(HttpServletResponse.SC_OK);
  }

  //로그인 실패시 실행하는 메소드
  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {

    log.error("로그인 실패: {}", failed.getMessage());

    // 응답 설정
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    ErrorResponse apiResponse = ErrorResponse.getResponse(ErrorCode.DUPLICATE_USERNAME.getCode(),ErrorCode.DUPLICATE_USERNAME.getMessage());
    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }
}
