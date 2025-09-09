package com.wardk.meeteam_backend.web.chat.dto;

import com.wardk.meeteam_backend.domain.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class
ChatMessageResponse {
  private List<ChatMessage> messages;
  private Long nextCursor;
}