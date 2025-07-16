package com.wardk.meeteam_backend.global.util;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.loginRegister.service.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.time.LocalDateTime;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

  private final CustomUserDetailsService customUserDetailsService;

  @Value("${jwt.secret-key}")
  private String secretKey;

  @Value("${jwt.access-exp-time}")
  private Long accessTokenExpTime; // AccessToken 만료 시간

  @Value("${jwt.refresh-exp-time}")
  private Long refreshTokenExpTime; // RefreshToken 만료 시간

  private static final String ACCESS_CATEGORY = "access";
  private static final String REFRESH_CATEGORY = "refresh";

  // 토큰에서 username 파싱
  public String getUsername(String token) {
    return Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("username", String.class);
  }

  // 토큰에서 role 파싱
  public String getRole(String token) {
    return Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("role", String.class);
  }

  // 토큰 만료 여부 확인
  public Boolean isExpired(String token) {
    return Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration()
            .before(new Date());
  }

  // Access/Refresh 토큰 여부
  public String getCategory(String token) {
    return Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("category", String.class);
  }

  /**
   * AccessToken 생성
   *
   * @param customSecurityUserDetails
   * @return
   */
  public String createAccessToken(CustomSecurityUserDetails customSecurityUserDetails) {
    log.info("엑세스 토큰 생성 중: 회원: {}", customSecurityUserDetails.getUsername());
    return createToken(ACCESS_CATEGORY, customSecurityUserDetails, accessTokenExpTime);
  }

  /**
   * RefreshToken 생성
   *
   * @param customSecurityUserDetails
   * @return
   */
  public String createRefreshToken(CustomSecurityUserDetails customSecurityUserDetails) {
    log.info("리프래시 토큰 생성 중: 회원: {}", customSecurityUserDetails.getUsername());
    return createToken(REFRESH_CATEGORY, customSecurityUserDetails, refreshTokenExpTime);
  }

  /**
   * JWT 토큰 생성 메서드
   *
   * @param customSecurityUserDetails 회원 상세 정보
   * @param expiredAt         만료 시간
   * @return 생성된 JWT 토큰
   */
  private String createToken(String category, CustomSecurityUserDetails customSecurityUserDetails, Long expiredAt) {

    return Jwts.builder()
            .subject(customSecurityUserDetails.getUsername())
            .claim("category", category)
            .claim("username", customSecurityUserDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiredAt))
            .signWith(getSignKey())
            .compact();
  }

  /**
   * JWT 토큰 유효성 검사
   *
   * @param token 검증할 JWT 토큰
   * @return 유효 여부
   */
  public boolean validateToken(String token) throws ExpiredJwtException {
    try {
      Jwts.parser()
              .verifyWith(getSignKey())
              .build()
              .parseSignedClaims(token);
      log.info("JWT 토큰이 유효합니다.");
      return true;
    } catch (ExpiredJwtException e) {
      log.warn("JWT 토큰이 만료되었습니다: {}", e.getMessage());
      throw e; // 만료된 토큰 예외를 호출한 쪽으로 전달
    } catch (UnsupportedJwtException e) {
      log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.warn("형식이 잘못된 JWT 토큰입니다: {}", e.getMessage());
    } catch (SignatureException e) {
      log.warn("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.warn("JWT 토큰이 비어있거나 null입니다: {}", e.getMessage());
    }
    return false;
  }

  /**
   * JWT 서명에 사용할 키 생성
   *
   * @return SecretKey 객체
   */
  private SecretKey getSignKey() {
    try {
      // Base64 문자열로부터 SecretKey를 생성
      byte[] keyBytes = Decoders.BASE64.decode(secretKey);
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (IllegalArgumentException e) {
      log.error("비밀 키 생성 실패: {}", e.getMessage());
      throw e; // 예외 재발생
    }
  }

  /**
   * JWT 토큰에서 클레임 (Claims) 추출
   *
   * @param token JWT 토큰
   * @return 추출된 클레임
   */
  public Claims getClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }

  /**
   * 액세스 토큰의 남은 만료 시간 반환
   *
   * @param token JWT 액세스 토큰
   * @return 남은 만료 시간 (밀리초)
   */
  public long getAccessTokenExpirationTime(String token) {
    Claims claims = getClaims(token);
    Date expirationDate = claims.getExpiration();
    long remainingTime = expirationDate.getTime() - System.currentTimeMillis();
    return Math.max(remainingTime, 0);
  }

  /**
   * 리프레시 토큰 만료 시간 반환
   *
   * @return 리프레시 토큰 만료 시간 (밀리초 단위)
   */
  public long getRefreshExpirationTime() {
    return refreshTokenExpTime;
  }

  /**
   * 리프레시 토큰 만료 날짜 반환
   *
   * @return 리프레시 토큰 만료 날짜
   */
  public LocalDateTime getRefreshExpiryDate() {
    return LocalDateTime.now().plusSeconds(refreshTokenExpTime / 1000);
  }

  /**
   * JWT 토큰에서 Authentication 객체 생성
   *
   * @param token JWT 토큰
   * @return Authentication 객체
   */
  public Authentication getAuthentication(String token) {
    Claims claims = getClaims(token);
    String username = claims.getSubject();
    log.info("JWT에서 인증정보 파싱: username={}", username);
    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
  }

  /**
   * OAuth2 사용자를 위한 AccessToken 생성 (이메일 기반)
   *
   * @param email 사용자 이메일
   * @param name 사용자 이름
   * @return JWT AccessToken
   */

  public String createAccessTokenForOAuth2(Member member) {
    if (member == null) {
      throw new IllegalArgumentException("Member cannot be null");
    }
    if (member.getEmail() == null || member.getEmail().isEmpty()) {
      throw new IllegalArgumentException("Member email cannot be null or empty");
    }
    if (member.getRole() == null) {
      throw new IllegalArgumentException("Member role cannot be null");
    }

    log.info("OAuth2 엑세스 토큰 생성 중: 회원: {}", member.getEmail());

    return Jwts.builder()
            .subject(member.getEmail())
            .claim("category", ACCESS_CATEGORY)
            .claim("username", member.getEmail())
            .claim("role", member.getRole().name())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpTime))
            .signWith(getSignKey())
            .compact();
  }

  public String createAccessTokenForOAuth2Email(String email, String name) {
    if (email == null || email.isEmpty()) {
      throw new IllegalArgumentException("Email cannot be null or empty");
    }

    log.info("OAuth2 엑세스 토큰 생성 중: 이메일: {}", email);

    return Jwts.builder()
            .subject(email)
            .claim("category", ACCESS_CATEGORY)
            .claim("username", email)
            .claim("role", "USER") // 기본 역할 설정
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpTime))
            .signWith(getSignKey())
            .compact();
  }

}
