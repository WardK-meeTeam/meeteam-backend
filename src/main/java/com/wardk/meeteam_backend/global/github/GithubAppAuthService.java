package com.wardk.meeteam_backend.global.github;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class GithubAppAuthService {

    private final WebClient.Builder webClientBuilder;

    @Value("${github.app.id}")
    private String appId;

    @Value("${github.app.private-key-pem}")
    private String privateKeyPem;

    private final ConcurrentHashMap<Long, String> tokenCache = new ConcurrentHashMap<>();

    public String getInstallationToken(Long installationId) {

        if(tokenCache.containsKey(installationId)) {
            return tokenCache.get(installationId);
        }

        String appJwt = createAppJwt();
        String token = requestInstallationToken(installationId, appJwt);

        tokenCache.put(installationId, token);
        return token;
    }


    private String createAppJwt() {
        try {
            RSAPrivateKey key = loadPrivateKeyFromPem(privateKeyPem);

            long nowSec = Instant.now().getEpochSecond();
            Algorithm alg = Algorithm.RSA256(null, key);

            return JWT.create()
                    .withIssuer(appId)
                    .withIssuedAt(new Date(nowSec * 1000))
                    .withExpiresAt(new Date((nowSec + 9 * 60) * 1000))
                    .sign(alg);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FAILED_TO_CREATE_APP_JWT);
        }
    }

    private String requestInstallationToken(Long installationId, String appJwt) {
        JsonNode resp = webClientBuilder.baseUrl("https://api.github.com").build()
                .post()
                .uri("/app/installations/{id}/access_tokens", installationId)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(appJwt))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (resp == null || resp.path("token").isMissingNode()) {
            log.error("Installation access token 발급 실패 resp={}", resp);
            throw new CustomException(ErrorCode.WEBHOOK_PROCESSING_ERROR);
        }

        return resp.get("token").asText();
    }

    private static RSAPrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
        String body = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(body);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }
}
