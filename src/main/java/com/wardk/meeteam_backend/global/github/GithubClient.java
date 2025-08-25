package com.wardk.meeteam_backend.global.github;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class GithubClient {

    private final WebClient webClient;

    public GithubClient(WebClient.Builder builder, @Value("${github.token}") String token) {
        this.webClient = builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "meeteam-pr-ingestion")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    public JsonNode get(String path, Object... uriVars) {
        return webClient.get()
                .uri(path, uriVars)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode[] getArray(String path, Object... uriVars) {
        return webClient.get()
                .uri(path, uriVars)
                .retrieve()
                .bodyToMono(JsonNode[].class)
                .block();
    }
}
