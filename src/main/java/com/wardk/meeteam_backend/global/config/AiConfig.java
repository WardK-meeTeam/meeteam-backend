package com.wardk.meeteam_backend.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.model:gpt-4o-mini}")
    private String model;

    /**
     * OpenAI 전용 WebClient (커넥션 풀/타임아웃/압축/Keep-Alive 설정)
     */
    @Bean
    public WebClient openAiWebClient() {
        // 커넥션 풀: 대기시간/타임아웃 감소
        ConnectionProvider provider = ConnectionProvider.builder("openai-pool")
                .maxConnections(30)  // 50→30으로 적정화
                .pendingAcquireMaxCount(60)  // 100→60으로 적정화
                .pendingAcquireTimeout(Duration.ofSeconds(5))  // 10→5초로 단축
                .maxIdleTime(Duration.ofSeconds(20))  // 30→20초로 단축
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .compress(true) // gzip
                .keepAlive(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 10s→5s로 단축
                .responseTimeout(Duration.ofSeconds(20)) // 18s→8s로 대폭 단축 (가드 타임아웃보다 작게)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(20)) // 18s→8s로 대폭 단축
                        .addHandlerLast(new WriteTimeoutHandler(20))); // 10s→5s로 단축

        return WebClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.USER_AGENT, "MeeTeam-Backend/1.0")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB 제한
                .build();
    }

     /**
     * OpenAI API 클라이언트
     */
    @Bean
    public OpenAiApi openAiApi(WebClient openAiWebClient) {
        return OpenAiApi.builder()
                .apiKey(openaiApiKey)
                .baseUrl("https://api.openai.com")
                .webClientBuilder(openAiWebClient().mutate())
                .build();
    }

    /**
     * 기본 Chat 옵션: 모델/토큰/스트리밍 등
     * - temperature 0.0으로 일관성 확보
     */
    @Bean
    public OpenAiChatOptions openAiDefaultOptions() {
        return OpenAiChatOptions.builder()
                .model("gpt-4o-mini") // 더 빠른 모델 사용
                .temperature(0.0) // 창의성 제어 (일관성 우선)
                .topP(0.8) // 다양성 약간 제한
                .maxTokens(2048) // 토큰 수 절반으로 축소 (빠른 응답)
                .presencePenalty(0.0)
                .frequencyPenalty(0.0)
                .build();
    }


    @Bean
    public ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder()
                .build();
    }

    /* 
     * OpenAI Chat 모델
     */
    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi,
            OpenAiChatOptions openAiDefaultOptions,
            ToolCallingManager toolCallingManager) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(openAiDefaultOptions)
                .toolCallingManager(toolCallingManager)
                .build();
    }

    /**
     * ChatClient (Spring AI 고수준 클라이언트)
     */
    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

}
