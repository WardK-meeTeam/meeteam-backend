package com.wardk.meeteam_backend.global.github;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GithubClient {

    private final WebClient webClient;

    public GithubClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "meeteam-pr-ingestion")
                .build();
    }

    public JsonNode get(String token, String path, Object... uriVars) {
        return webClient.get()
                .uri(path, uriVars)
                .headers(h -> {
                    if(token != null && !token.isEmpty())
                        h.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode[] getArray(String token, String path, Object... uriVars) {
        final int PER_PAGE = 100;
        int page = 1;

        List<JsonNode> list = new ArrayList<>();

        while (true) {
            final int currentPage = page;
            ResponseEntity<JsonNode[]> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("per_page", PER_PAGE)
                            .queryParam("page", currentPage)
                            .build(uriVars))
                    .headers(h -> {
                        if(token != null && !token.isEmpty())
                            h.setBearerAuth(token);
                    })
                    .retrieve()
                    .toEntity(JsonNode[].class)
                    .block();

            if (response == null) break;

            JsonNode[] body = response.getBody();
            if (body == null || body.length == 0) break;

            for (JsonNode node : body) {
                list.add(node);
            }

            String link = response.getHeaders().getFirst("Link");
            boolean hasNext = (link != null && link.contains("rel=\"next\""));
            boolean lastBySize = (body.length < PER_PAGE);

            if(!hasNext && lastBySize) break;

            page++;
        }

        return list.toArray(new JsonNode[0]);
    }
}
