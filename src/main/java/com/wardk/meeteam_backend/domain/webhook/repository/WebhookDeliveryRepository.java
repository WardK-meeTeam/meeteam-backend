package com.wardk.meeteam_backend.domain.webhook.repository;

import com.wardk.meeteam_backend.domain.webhook.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, String> { }
