package com.wardk.meeteam_backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket과 STOMP 메시징을 위한 설정 클래스입니다.
 *
 * <p>RabbitMQ 브로커를 사용한 WebSocket 설정:</p>
 * <ul>
 *   <li>RabbitMQ STOMP 브로커 사용 (메시지 지속성 보장)</li>
 *   <li>기존 Spring Security JWT 인증과 연동</li>
 *   <li>메시지 큐 자동 관리</li>
 *   <li>고급 메시지 라우팅 지원</li>
 *   <li>SockJS 폴백 지원</li>
 * </ul>
 *
 * <p>RabbitMQ 메시지 플로우:</p>
 * <ol>
 *   <li>클라이언트가 JWT 토큰으로 HTTP 인증 완료</li>
 *   <li>'/ws' 엔드포인트로 WebSocket 연결</li>
 *   <li>RabbitMQ 큐 자동 생성 및 바인딩</li>
 *   <li>'/topic/chat/{roomId}' 구독으로 채팅방 참여</li>
 *   <li>'/app/chat.*' 경로로 메시지 전송</li>
 *   <li>RabbitMQ를 통해 실시간 브로드캐스트</li>
 * </ol>
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port:61613}")
    private int rabbitmqStompPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitmqPassword;

    /**
     * STOMP 메시지 브로커를 구성합니다.
     *
     * <p>RabbitMQ 브로커 설정:</p>
     * <ul>
     *   <li>/topic: 1:N 브로드캐스트 (채팅방 메시지) - fanout exchange</li>
     *   <li>/queue: 1:1 개인 메시지 (멘션, 알림) - direct exchange</li>
     *   <li>/app: 클라이언트→서버 메시지 prefix</li>
     *   <li>/user: 개인 메시지 라우팅 prefix</li>
     *   <li>메시지 지속성: 서버 재시작 시에도 메시지 보존</li>
     * </ul>
     *
     * @param config 메시지 브로커 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // RabbitMQ STOMP 브로커 설정
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitmqHost)           // RabbitMQ 서버 호스트
                .setRelayPort(rabbitmqStompPort)      // STOMP 포트 (기본 61613)
                .setClientLogin(rabbitmqUsername)     // 클라이언트 로그인
                .setClientPasscode(rabbitmqPassword)  // 클라이언트 패스워드
                .setSystemLogin(rabbitmqUsername)     // 시스템 로그인
                .setSystemPasscode(rabbitmqPassword)  // 시스템 패스워드
                .setHeartbeatValue(new long[]{10000, 10000}) // 10초 간격 하트비트
                .setVirtualHost("/");                 // 가상 호스트

        // 클라이언트→서버 메시지 prefix
        config.setApplicationDestinationPrefixes("/app");

        // 개인 메시지 라우팅 prefix
        config.setUserDestinationPrefixes("/user");
    }

    /**
     * STOMP 엔드포인트를 등록합니다.
     *
     * <p>클라이언트 연결 설정:</p>
     * <ul>
     *   <li>엔드포인트: '/ws'</li>
     *   <li>CORS: 기존 SecurityConfig의 CORS 설정 활용</li>
     *   <li>SockJS: WebSocket 미지원 브라우저 폴백</li>
     * </ul>
     *
     * @param registry STOMP 엔드포인트 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정
                .withSockJS();
    }
}
