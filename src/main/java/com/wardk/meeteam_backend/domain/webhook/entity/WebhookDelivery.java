package com.wardk.meeteam_backend.domain.webhook.entity;

import com.wardk.meeteam_backend.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * GitHub Webhook 요청의 수신 및 처리 상태를 저장하는 엔티티
 * 
 * <p>
 * GitHub에서 발생한 이벤트(PR 생성, 코멘트 추가 등)가 webhook으로 전송될 때마다
 * 이 엔티티에 기록하여 중복 처리를 방지하고 처리 이력을 추적한다.
 * </p>
 * 
 * <p>
 * 각 Webhook 요청은 GitHub에서 제공하는 고유 deliveryId로 식별되며,
 * 원본 페이로드와 서명, 처리 상태 등을 함께 저장합니다.
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class WebhookDelivery extends BaseEntity {

    /**
     * PK
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * GitHub에서 생성한 고유 배달 ID (X-GitHub-Delivery 헤더 값)
     * 재시도 등의 이유로 중복 요청이 들어올 경우 이 값으로 식별하여 중복 처리를 방지합니다.
     */
    @Column(name = "delivery_id", nullable = false, unique = true)
    private String deliveryId;

    /**
     * GitHub 이벤트 유형 (X-GitHub-Event 헤더 값)
     * 예: pull_request, issue_comment, ping 등
     * 이 값에 따라 어떤 핸들러로 요청을 라우팅할지 결정합니다.
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * GitHub에서 제공한 서명 (X-Hub-Signature-256 헤더 값)
     * 요청 인증을 위해 GitHub에서 제공하는 HMAC-SHA256 서명값입니다.
     * 형식: sha256=hexdigest
     */
    @Column(name = "signature")
    private String signature;

    /**
     * Webhook 처리 상태
     * <p>
     * 기본값은 RECEIVED이며, 처리 결과에 따라 PROCESSED, DUPLICATE, FAILED로 변경됩니다.
     * </p>
     * 
     * @see Status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.RECEIVED;

    /**
     * GitHub에서 전송한 원본 JSON 페이로드
     * <p>
     * 디버깅 및 문제 추적을 위해 원본 페이로드를 저장합니다.
     * </p>
     * <p>
     * MEDIUMTEXT 타입을 사용하여 최대 16MB까지 저장 가능합니다.
     * </p>
     */
    @Column(name = "raw_payload", columnDefinition = "MEDIUMTEXT")
    private String rawPayload;

    /**
     * Webhook 요청을 수신한 시간
     * <p>
     * 초 단위까지 정확한 시간 기록
     * </p>
     */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    /**
     * Webhook 요청 처리가 완료된 시간
     * <p>
     * 처리 완료(PROCESSED) 또는 실패(FAILED) 시 설정됩니다.
     * </p>
     * <p>
     * 초 단위까지 정확한 시간 기록
     * </p>
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * 처리 실패 시 오류 메시지
     * <p>
     * status가 FAILED인 경우 실패 원인을 저장합니다.
     * </p>
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Webhook 요청 처리 상태를 나타내는 열거형
     */
    public enum Status {
        /**
         * 요청이 수신되었으나 아직 처리되지 않음
         */
        RECEIVED,

        /**
         * 요청이 성공적으로 처리 완료됨
         */
        PROCESSED,

        /**
         * 이미 처리된 중복 요청으로 식별됨 (같은 deliveryId)
         */
        DUPLICATE,

        /**
         * 요청 처리 중 오류 발생
         */
        FAILED
    }

    /**
     * 처리 완료 상태로 변경하고 처리 시간을 현재로 설정
     */
    public void markAsProcessed() {
        this.status = Status.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 처리 실패 상태로 변경하고 오류 메시지와 처리 시간을 설정
     * 
     * @param errorMessage 실패 원인 메시지
     */
    public void markAsFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 중복 요청 상태로 변경합니다.
     */
    public void markAsDuplicate() {
        this.status = Status.DUPLICATE;
    }
}