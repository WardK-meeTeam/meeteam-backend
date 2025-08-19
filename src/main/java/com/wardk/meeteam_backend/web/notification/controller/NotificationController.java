package com.wardk.meeteam_backend.web.notification.controller;

import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List; import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationRepository notificationRepository;

  @GetMapping
  public ResponseEntity<List<Notification>> listMine() {
    // TODO: 현재 사용자 기준으로 필터링
    return ResponseEntity.ok(notificationRepository.findAll());
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> markRead(@PathVariable UUID id) {
    // TODO: isRead=true 저장
    return ResponseEntity.noContent().build();
  }
}

