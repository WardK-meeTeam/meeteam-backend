package com.wardk.meeteam_backend.web.webhook.controller;

import com.wardk.meeteam_backend.domain.webhook.service.GithubWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
public class GithubWebhookController {

  //private final GithubWebhookService service;

  @PostMapping
  public ResponseEntity<Void> handle(
      @RequestHeader("X-Hub-Signature-256") String signature,
      @RequestHeader("X-GitHub-Event") String event,
      @RequestHeader("X-GitHub-Delivery") String deliveryId,
      @RequestBody byte[] payload) {
    //service.verifySignatureOrThrow(signature, payload);
    //service.ensureNotDuplicate(deliveryId, event);
    //service.dispatch(event, payload);
    return ResponseEntity.ok().build();
  }
}

