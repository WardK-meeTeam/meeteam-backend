package com.wardk.meeteam_backend.domain.webhook.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebhookDelivery {
  @Id
  private String deliveryId; // X-GitHub-Delivery

  private String event; // pull_request, pull_request_review, ...
  private Instant receivedAt;
}
