package com.wardk.meeteam_backend.global.auth.filter;



import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private static final PathMatcher pathMatcher = new AntPathMatcher();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

    // 인증 생략 경로
    String uri = request.getRequestURI();
    
    if (
            //uri.equals("/api/login") ||
            //uri.equals("/api/register") ||
            //uri.equals("/api/auth/oauth2/success") ||
            //uri.equals("/api/auth/oauth2/failure") ||

            uri.startsWith("/api/auth") ||
            uri.startsWith("/api/webhooks/github") ||

            uri.equals("/docs/swagger-ui/index.html") ||
            //uri.startsWith("/swagger-ui/**") ||
            //uri.equals("/swagger-ui.html") ||
            //uri.startsWith("/swagger-resources/**") ||
            uri.startsWith("/webjars/**") ||
            uri.startsWith("/v3/api-docs") ||
            //uri.startsWith("/docs/**") ||
                uri.startsWith("/docs") ||
            uri.startsWith("/actuator") ||
            uri.startsWith("/v3") ||

            uri.startsWith("/oauth2/") ||
            uri.startsWith("/login/oauth2/") ||

            uri.equals("/favicon.ico") ||
            uri.equals("/default-ui.css") ||

            uri.equals("/")
    ) {
      filterChain.doFilter(request, response);
      return;
    }



    // request에서 Authorization 헤더를 찾음
    String authorization = request.getHeader("Authorization");

    // Authorization 헤더 검증
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      log.error("토큰이 존재하지 않습니다.");
      filterChain.doFilter(request, response);

      return;
    }

    log.info("토큰이 존재합니다");

    // Bearer 부분 제거 후 순수 토큰만 획득
    String token = authorization.split(" ")[1];

    //토큰 소멸 시간 검증
    if (jwtUtil.isExpired(token)) {

      System.out.println("token expired");
      filterChain.doFilter(request, response);

      //조건이 해당되면 메소드 종료 (필수)
      return;
    }

    //토큰에서 username과 role 획득
    String email = jwtUtil.getUsername(token);

    //user를 생성하여 값 set
    Member member = Member.builder()
            .email(email)
            .build();

    //UserDetails에 회원 정보 객체 담기
    CustomSecurityUserDetails customSecurityUserDetails = new CustomSecurityUserDetails(member);

    //스프링 시큐리티 인증 토큰 생성
    Authentication authToken = new UsernamePasswordAuthenticationToken(customSecurityUserDetails, null, customSecurityUserDetails.getAuthorities());
    //세션에 사용자 등록
    SecurityContextHolder.getContext().setAuthentication(authToken);

    filterChain.doFilter(request, response);
  }
}
