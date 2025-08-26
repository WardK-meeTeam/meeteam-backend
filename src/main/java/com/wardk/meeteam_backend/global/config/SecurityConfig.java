package com.wardk.meeteam_backend.global.config;

import com.wardk.meeteam_backend.global.exception.RestAccessDeniedHandler;
import com.wardk.meeteam_backend.global.exception.RestAuthenticationEntryPoint;
import com.wardk.meeteam_backend.global.auth.filter.JwtFilter;
import com.wardk.meeteam_backend.global.auth.filter.LoginFilter;
import com.wardk.meeteam_backend.global.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    /**
     * Security Filter Chain 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 로그인 경로를 설정하기 위해 LoginFilter 생성
        LoginFilter loginFilter = new LoginFilter(jwtUtil, authenticationManager(authenticationConfiguration));
        loginFilter.setFilterProcessesUrl("/api/login"); // TODO: 로그인 경로 커스텀 "/api/auth/login"
        //->경로를 커스텀 할 수 있다.
        return http
                // cors 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // csrf disable
                .csrf(AbstractHttpConfigurer::disable)
                // http basic 인증 방식 disable
                .httpBasic(AbstractHttpConfigurer::disable)
                // form 로그인 방식 disable
                .formLogin(AbstractHttpConfigurer::disable)
                // ★ 2) 인증 실패 시 401 반환 (기본 /login 리다이렉트 막기)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        )
                )
                // ★ 미인증 진입 시 401로 JSON/빈 응답 처리, 권한 부족 시 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint) // ★ 401을 ErrorResponse로
                        .accessDeniedHandler(restAccessDeniedHandler)           // ★ 403을 ErrorResponse로
                )
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/webjars/**", "/swagger-resources/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**",
                                "/docs/**", "/default-ui.css", "/favicon.ico",
                                "/api/login",
                                "/api/auth/**", "/", "/uploads/**", "/api/register", "/api/project/register", "/oauth2/**", "/login/oauth2/**", "/login/oauth2/code/**",
                                "/api/auth/oauth2/success", "/api/auth/oauth2/failure",
                                "/api/webhooks/github").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated() //나머지는 인증이 된 사용자만 가능
                )
                // OAuth 2.0 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler) // OAuth 성공 후 핸들러 설정
                        .failureUrl("/api/auth/oauth2/failure") // OAuth 실패 후 리다이렉트 URL
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // 커스텀 OAuth2UserService 사용
                        )
                )
//            .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/login", "/api/auth/join").permitAll() // 로그인, 회원가입은 누구나 접근 가능
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // /api/admin/** 경로는 ROLE_ADMIN만 접근 가능
//                        .anyRequest().authenticated()
                // 세션 설정 STATELESS 에서 세션 설정 수정 - OAuth2 사용시 IF_REQUIRED 필요
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .addFilterBefore(
                        new JwtFilter(jwtUtil),
                        LoginFilter.class
                )
                .addFilterAt(
                        loginFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    /**
     * 인증 메니저 설정
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration)
            throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * CORS 설정 소스 빈
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",// React 프론트
                "http://3.37.43.118:8080" // Swagger UI
        )); // 허용할 오리진 TODO: CORS 경로 설정 "http://localhost:3000"


        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 허용할 HTTP 메서드
        configuration.setAllowCredentials(true); // 인증 정보 포함 여부
        configuration.setAllowedHeaders(Collections.singletonList("*")); // 허용할 헤더
        configuration.setMaxAge(3600L); // Preflight 캐싱 시간

        // 모든 경로에 대해 CORS 설정 적용
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", configuration);
        return urlBasedCorsConfigurationSource;
    }

    /**
     * 비밀번호 인코더 빈 (BCrypt)
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        return firewall;
    }

}
