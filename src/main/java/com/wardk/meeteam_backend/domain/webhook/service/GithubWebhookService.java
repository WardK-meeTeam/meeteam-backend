package com.wardk.meeteam_backend.domain.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.domain.webhook.entity.WebhookDelivery;
import com.wardk.meeteam_backend.domain.webhook.repository.WebhookDeliveryRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public class GithubWebhookService {

    private final WebhookDeliveryRepository deliveryRepository;

    /**
     * Webhook 수신 기록을 저장합니다.
     * 
     * @param deliveryId GitHub에서 보낸 X-GitHub-Delivery 값
     * @param eventType GitHub 이벤트 타입 (pull_request, issue_comment 등)
     * @param signature GitHub 서명 (X-Hub-Signature-256)
     * @param rawPayload 원본 요청 바디
     * @return 저장된 WebhookDelivery 엔티티
     */
    public WebhookDelivery recordWebhook(String deliveryId, String eventType, String signature, byte[] rawPayload) {
        log.info("Webhook 기록 시작: deliveryId={}, eventType={}", deliveryId, eventType);
        
        // 중복 체크
        Optional<WebhookDelivery> existingDelivery = deliveryRepository.findByDeliveryId(deliveryId);
        
        if (existingDelivery.isPresent()) {
            log.info("중복 Webhook 감지: deliveryId={}", deliveryId);
            WebhookDelivery delivery = existingDelivery.get();
            delivery.setStatus(WebhookDelivery.Status.DUPLICATE);
            return deliveryRepository.save(delivery);
        }
        
        // 새 Webhook 수신 기록 생성
        log.debug("새로운 Webhook 기록 생성: deliveryId={}", deliveryId);
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setDeliveryId(deliveryId);
        delivery.setEventType(eventType);
        delivery.setSignature(signature);
        delivery.setRawPayload(new String(rawPayload));
        delivery.setStatus(WebhookDelivery.Status.RECEIVED);
        delivery.setReceivedAt(LocalDateTime.now());
        
        WebhookDelivery saved = deliveryRepository.save(delivery);
        log.info("Webhook 기록 완료: id={}, deliveryId={}", saved.getId(), deliveryId);
        return saved;
    }
  
    /**
     * Webhook 이벤트를 타입에 따라 적절한 핸들러로 라우팅합니다.
     * 
     * @param eventType GitHub 이벤트 타입
     * @param payload JSON 페이로드
     */
    public void dispatch(String eventType, JsonNode payload) {
        if (eventType == null) {
            log.error("이벤트 타입이 null입니다");
            throw new CustomException(ErrorCode.WEBHOOK_PROCESSING_ERROR);
        }
        
        try {
            switch (eventType) {
                case "ping" -> handlePingEvent(payload);
                case "pull_request" -> handlePullRequestEvent(payload);
                case "issue_comment" -> handleIssueCommentEvent(payload);
                default -> log.info("미지원 이벤트 타입: {}, 기록만 저장합니다.", eventType);
            }
        } catch (Exception e) {
            log.error("Webhook 처리 중 오류 발생: type={}, error={}", eventType, e.getMessage(), e);
            throw new CustomException(ErrorCode.WEBHOOK_PROCESSING_ERROR);
        }
    }
    
    /**
     * Webhook 처리 완료 상태로 업데이트합니다.
     * 
     * @param id WebhookDelivery ID
     * @throws WebhookException 해당 ID의 WebhookDelivery가 존재하지 않을 경우
     */
    public void markProcessed(Long id) {
        WebhookDelivery delivery = deliveryRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("처리 완료 표시할 Webhook을 찾을 수 없음: id={}", id);
                return new CustomException(ErrorCode.WEBHOOK_DELIVERY_NOT_FOUND);
            });
        
        delivery.setStatus(WebhookDelivery.Status.PROCESSED);
        delivery.setProcessedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
        
        log.info("Webhook 처리 완료 상태로 업데이트: id={}, deliveryId={}", id, delivery.getDeliveryId());
    }
    
    /**
     * Webhook 처리 실패 상태로 업데이트합니다.
     * 
     * @param id WebhookDelivery ID
     * @param errorMessage 오류 메시지
     * @throws WebhookException 해당 ID의 WebhookDelivery가 존재하지 않을 경우
     */
    public void markFailed(Long id, String errorMessage) {
        WebhookDelivery delivery = deliveryRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("처리 실패 표시할 Webhook을 찾을 수 없음: id={}", id);
                return new CustomException(ErrorCode.WEBHOOK_DELIVERY_NOT_FOUND);
            });
        
        delivery.setStatus(WebhookDelivery.Status.FAILED);
        delivery.setErrorMessage(errorMessage);
        delivery.setProcessedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
        
        log.info("Webhook 처리 실패 상태로 업데이트: id={}, deliveryId={}", id, delivery.getDeliveryId());
    }
    
    // 이벤트 핸들러 메서드들
    private void handlePingEvent(JsonNode payload) {
        String zen = payload.path("zen").asText("zen 값 없음");
        String hookId = payload.path("hook_id").asText("hook_id 없음");
        log.info("GitHub Ping 이벤트 수신: zen={}, hook_id={}", zen, hookId);
    }
    
    private void handlePullRequestEvent(JsonNode payload) {
        String action = payload.path("action").asText("action 없음");
        JsonNode pr = payload.path("pull_request");
        String prTitle = pr.path("title").asText("제목 없음");
        String prNumber = pr.path("number").asText("번호 없음");
        String repoName = payload.path("repository").path("full_name").asText("저장소 이름 없음");
        
        log.info("Pull Request 이벤트: action={}, repo={}, PR #{}: {}",
                action, repoName, prNumber, prTitle);
        
        // 실제 비즈니스 로직 구현
        switch (action) {
            case "opened", "synchronize" -> handlePullRequestUpdate(pr, repoName);
            case "closed" -> {
                if (pr.path("merged").asBoolean(false)) {
                    handlePullRequestMerged(pr, repoName);
                } else {
                    handlePullRequestClosed(pr, repoName);
                }
            }
            default -> log.debug("지원하지 않는 PR 액션: {}", action);
        }
    }
    
    private void handlePullRequestUpdate(JsonNode pr, String repoName) {
        // PR 생성 또는 업데이트 로직
        log.debug("PR 생성/업데이트 처리: repo={}, prNumber={}", 
                repoName, pr.path("number").asText());
        // TODO: 실제 구현
    }
    
    private void handlePullRequestMerged(JsonNode pr, String repoName) {
        // PR 병합 로직
        log.debug("PR 병합 처리: repo={}, prNumber={}", 
                repoName, pr.path("number").asText());
        // TODO: 실제 구현
    }
    
    private void handlePullRequestClosed(JsonNode pr, String repoName) {
        // PR 닫힘 로직
        log.debug("PR 닫힘 처리: repo={}, prNumber={}", 
                repoName, pr.path("number").asText());
        // TODO: 실제 구현
    }
    
    private void handleIssueCommentEvent(JsonNode payload) {
        String action = payload.path("action").asText("action 없음");
        String comment = payload.path("comment").path("body").asText("댓글 내용 없음");
        String issueNumber = payload.path("issue").path("number").asText("이슈 번호 없음");
        String repoName = payload.path("repository").path("full_name").asText("저장소 이름 없음");
        
        log.info("Issue Comment 이벤트: action={}, repo={}, Issue #{}, comment: {}",
                action, repoName, issueNumber, comment);
        
        // 코멘트 처리 로직
        if ("created".equals(action)) {
            processNewComment(comment, issueNumber, repoName, payload);
        }
    }
    
    private void processNewComment(String comment, String issueNumber, String repoName, JsonNode payload) {
        // 특정 명령어 감지 및 처리
        log.debug("새 댓글 처리: repo={}, issue={}", repoName, issueNumber);
        
        // 예: 코드 리뷰 요청 명령 감지
        if (comment.contains("/review")) {
            log.info("코드 리뷰 요청 감지: repo={}, issue={}", repoName, issueNumber);
            // TODO: 코드 리뷰 로직 구현
        }
        
        // 예: 배포 명령 감지
        if (comment.contains("/deploy")) {
            log.info("배포 명령 감지: repo={}, issue={}", repoName, issueNumber);
            // TODO: 배포 로직 구현
        }
    }
}

