package com.wardk.meeteam_backend.global.config;

import com.wardk.meeteam_backend.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * WebSocket 연결 시 JWT 토큰 인증을 처리하는 인터셉터
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketJwtAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && isValidToken(token)) {
                try {
                    Long userId = jwtUtil.getMemberId(token);
                    String username = jwtUtil.getUsername(token);
                    String role = jwtUtil.getRole(token);

                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(role))
                    );

                    accessor.setUser(authentication);
                    log.info("WebSocket JWT 인증 성공 - 사용자 ID: {}, 사용자명: {}", userId, username);

                } catch (Exception e) {
                    log.error("WebSocket JWT 인증 처리 중 오류 발생", e);
                    throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다");
                }
            } else {
                log.warn("WebSocket 연결 시도 - 유효하지 않은 JWT 토큰");
                throw new IllegalArgumentException("유효한 JWT 토큰이 필요합니다");
            }
        }

        return message;
    }

    private boolean isValidToken(String token) {
        try {
            if (jwtUtil.isExpired(token)) {
                log.warn("만료된 JWT 토큰");
                return false;
            }

            String category = jwtUtil.getCategory(token);
            if (!"access".equals(category)) {
                log.warn("Access 토큰이 아님: {}", category);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 오류 발생", e);
            return false;
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String tokenParam = accessor.getFirstNativeHeader("token");
        if (tokenParam != null) {
            return tokenParam;
        }

        log.debug("JWT 토큰을 찾을 수 없습니다");
        return null;
    }
}
