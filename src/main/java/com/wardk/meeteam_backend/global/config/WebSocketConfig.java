package com.wardk.meeteam_backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket과 STOMP 메시징을 위한 설정 클래스
 * 기존 REST API 채팅과 함께 실시간 WebSocket 채팅 기능 제공
 *
 * @author MeeTeam Backend Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Value("${spring.rabbitmq.host:rabbitmq}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int rabbitmqStompPort;

    @Value("${spring.rabbitmq.username:meeteam}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password:meeteam123}")
    private String rabbitmqPassword;

    /**
     * RabbitMQ STOMP 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // RabbitMQ STOMP 브로커 설정
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitmqHost)
                .setRelayPort(rabbitmqStompPort)
                .setClientLogin(rabbitmqUsername)
                .setClientPasscode(rabbitmqPassword)
                .setSystemLogin(rabbitmqUsername)
                .setSystemPasscode(rabbitmqPassword)
                .setSystemHeartbeatSendInterval(10000)
                .setSystemHeartbeatReceiveInterval(10000)
                .setVirtualHost("/")
                // RabbitMQ STOMP destination 변환 설정
                .setAutoStartup(true);

        // 클라이언트→서버 메시지 prefix
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
        
        // RabbitMQ에서 사용할 destination prefix 추가 설정
        config.setPreservePublishOrder(true);
    }

    /**
     * WebSocket 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * JWT 인증 인터셉터 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtAuthenticationInterceptor);
    }
}
