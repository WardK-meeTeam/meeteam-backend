package com.wardk.meeteam_backend.domain.chat.service;

import java.util.UUID;

public interface ChatService {
  void appendUserMessage(UUID threadId, String userEmail, String text);
}
