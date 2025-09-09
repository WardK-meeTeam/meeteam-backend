package com.wardk.meeteam_backend.global.auth.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoginFilterTest {

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        //given
        JwtUtil jwtUtil = Mockito.mock(JwtUtil.class);
        AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);

        LoginFilter filter = new LoginFilter(jwtUtil, authenticationManager);
        filter.setFilterProcessesUrl("/api/auth/login");

        CustomSecurityUserDetails principal = Mockito.mock(CustomSecurityUserDetails.class);
        Mockito.when(principal.getUsername())
                .thenReturn("test@naver.com");

        UsernamePasswordAuthenticationToken successAuth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        successAuth.setDetails(null);

        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(successAuth);

        Mockito.when(jwtUtil.createAccessToken(principal)).thenReturn("ACCESS_TOKEN");
        Mockito.when(jwtUtil.createRefreshToken(principal)).thenReturn("REFRESH_TOKEN");
        Mockito.when(jwtUtil.getRefreshExpirationTime()).thenReturn(3600_000L);

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "email", "test@naver.com",
                "password", "password"
        ));

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setContentType("application/json");
        req.setContent(body);
        req.setServletPath("/api/auth/login");

        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        //when
        filter.doFilter(req, resp, chain);

        //then
        assertEquals(200, resp.getStatus());

        String authHeader = resp.getHeader("Authorization");
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Bearer "));
        assertEquals("Bearer ACCESS_TOKEN", authHeader);

        Cookie[] cookies = resp.getCookies();
        assertNotNull(cookies);
        Cookie refreshToken = Arrays.stream(cookies)
                .filter(c -> c.getName().equals("refreshToken"))
                .findFirst().orElse(null);

        assertNotNull(refreshToken);
        assertEquals("REFRESH_TOKEN", refreshToken.getValue());

        String content = resp.getContentAsString(StandardCharsets.UTF_8);
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }
}