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

    @Value("${spring.ai.openai.model:gpt-4o}")
    private String model;

    /**
     * OpenAI 전용 WebClient (커넥션 풀/타임아웃/압축/Keep-Alive 설정)
     */
    @Bean
    public WebClient openAiWebClient() {
        // 커넥션 풀: 대기시간/타임아웃 감소
        ConnectionProvider provider = ConnectionProvider.builder("openai-pool")
                .maxConnections(200)
                .pendingAcquireMaxCount(1000)
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .compress(true) // gzip
                .keepAlive(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000) // TCP connect 15s
                .responseTimeout(Duration.ofSeconds(60)) // 첫 바이트까지 60s
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60)) // read 60s
                        .addHandlerLast(new WriteTimeoutHandler(60))); // write 60s

        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1") // 바로 /v1로
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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
     * - stream 기본 ON: 첫 바이트가 빨리 와서 ReadTimeout 리스크↓
     * - maxTokens은 상황에 맞게 조정
     */
    @Bean
    public OpenAiChatOptions openAiDefaultOptions() {
        return OpenAiChatOptions.builder()
                .model("gpt-4o") // 모델명
                .temperature(0.1) // 창의성 제어
                .topP(1.0) // 다양성 제어
                .maxTokens(1024) // 서비스 특성에 맞춰 조정
                .presencePenalty(0.0)
                .frequencyPenalty(0.0)
                // .responseFormat() // 반환 타입 적용
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
