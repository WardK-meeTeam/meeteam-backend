package com.wardk.meeteam_backend.web.chat.controller;

import com.wardk.meeteam_backend.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

  //private final ChatService chatService;

  @MessageMapping("/chat.send")
  public void send(UserChatPayload payload, Principal principal) {
    // TODO: principal.getName() 으로 사용자 식별
    //chatService.appendUserMessage(payload.threadId(), principal != null ? principal.getName() : "anonymous", payload.text());
  }

  public record UserChatPayload(java.util.UUID threadId, String text) { }
}

