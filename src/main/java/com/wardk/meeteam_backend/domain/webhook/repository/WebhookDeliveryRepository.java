package com.wardk.meeteam_backend.domain.webhook.repository;

import com.wardk.meeteam_backend.domain.webhook.entity.WebhookDelivery;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {
    /**
     * GitHub에서 보낸 deliveryId로 기존 WebHookDelivery를 찾습니다.
     * 중복 요청 처리에 사용됩니다.
     */
    Optional<WebhookDelivery> findByDeliveryId(String deliveryId);

    /**
     * 특정 deliveryId가 존재하는지 확인합니다.
     */
    boolean existsByDeliveryId(String deliveryId);
}
