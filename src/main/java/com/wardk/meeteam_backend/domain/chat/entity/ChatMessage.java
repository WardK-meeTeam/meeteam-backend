package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant; import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = @Index(columnList = "thread_id, createdAt"))
public class ChatMessage {

  public enum Role { SYSTEM, USER, ASSISTANT, TOOL }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private ChatThread thread;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY)
  private Member author; // USER 메시지일 때 사용

  @Column(columnDefinition = "text")
  private String content;

  private String modelName; private Integer promptTokens; private Integer completionTokens;

  private Instant createdAt;
}
