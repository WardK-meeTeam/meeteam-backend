package com.wardk.meeteam_backend.domain.webhook.service;

public interface GithubWebhookService {
  void verifySignatureOrThrow(String signatureHeader, byte[] rawPayload);
  void ensureNotDuplicate(String deliveryId, String event);
  void dispatch(String event, byte[] payload);
}

