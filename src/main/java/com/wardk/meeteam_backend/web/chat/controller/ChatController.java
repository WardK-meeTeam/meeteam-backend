package com.wardk.meeteam_backend.web.chat.controller;


import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import com.wardk.meeteam_backend.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List; import java.util.UUID;

@RestController
@RequestMapping("/threads")
@RequiredArgsConstructor
public class ChatController {

  private final ChatMessageRepository messageRepo;
  //private final ChatService chatService;

  @GetMapping("/{threadId}/messages")
  @PreAuthorize("@perm.canAccessThread(#threadId, authentication)")
  public ResponseEntity<List<ChatMessage>> list(@PathVariable UUID threadId) {
    // TODO: 페이징/DTO 변환은 후속 이슈
    return ResponseEntity.ok(messageRepo.findByThreadIdOrderByCreatedAtAsc(threadId));
  }

  @PostMapping("/{threadId}/messages")
  @PreAuthorize("@perm.canAccessThread(#threadId, authentication)")
  public ResponseEntity<Void> send(@PathVariable UUID threadId, @RequestBody SendReq req) {
    // TODO: SecurityContext에서 추출
    //chatService.appendUserMessage(threadId, "CURRENT_USER_EMAIL", req.text());
    return ResponseEntity.accepted().build();
  }

  public record SendReq(String text) { }
}

