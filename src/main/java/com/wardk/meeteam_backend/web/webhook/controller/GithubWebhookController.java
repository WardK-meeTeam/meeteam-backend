package com.wardk.meeteam_backend.web.webhook.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardk.meeteam_backend.domain.webhook.entity.WebhookDelivery;
import com.wardk.meeteam_backend.domain.webhook.service.GithubWebhookService;
import com.wardk.meeteam_backend.domain.webhook.util.SignatureVerifier;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/github")
@RequiredArgsConstructor
public class GithubWebhookController {

    private final GithubWebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    /**
     * GitHub Webhook 요청을 처리하는 엔드포인트
     * 서명 검증은 X-Hub-Signature-256 헤더가 있는 경우에만 수행합니다.
     */
    @PostMapping
    public ResponseEntity<?> handleWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "X-GitHub-Event", required = true) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = true) String deliveryId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        try {
            // 요청 바디 원본 바이트 읽기
            byte[] rawBody = request.getInputStream().readAllBytes();

            // 서명 검증
            if (signature != null) {
                if (!SignatureVerifier.validateSignature(rawBody, signature, webhookSecret)) {
                    log.warn("GitHub Webhook 서명 검증 실패: {}", deliveryId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Collections.singletonMap("error", "Invalid signature"));
                }
                log.info("서명 검증 성공");
            } else {
                log.warn("서명 헤더 없음 - 검증 생략: {}", deliveryId);
                // throw new CustomException(ErrorCode.WEBHOOK_SIGNATURE_REQUIRED);
            }

            // JSON 파싱
            JsonNode payload = objectMapper.readTree(rawBody);

            Long installationId = payload.path("installation").path("id").asLong();
            log.info("event={}, installationId={}, deliveryId={}", eventType, installationId, deliveryId);

            // Webhook 수신 기록
            WebhookDelivery delivery = webhookService.recordWebhook(
                    deliveryId,
                    eventType,
                    signature != null ? signature : "no-signature",
                    rawBody);

            // 중복 요청 처리
            if (delivery.getStatus() == WebhookDelivery.Status.DUPLICATE) {
                log.info("중복 Webhook 요청 감지: {}", deliveryId);
                return ResponseEntity.ok().body(Collections.singletonMap("status", "duplicate"));
            }

            //  비동기 처리를 위해 이벤트 발행
            webhookService.dispatch(eventType, payload, installationId);

            // 처리 완료 표시
            webhookService.markProcessed(delivery.getId());

            return ResponseEntity.ok(Collections.singletonMap("status", "processed"));

        } catch (IOException e) {
            log.error("Webhook 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}