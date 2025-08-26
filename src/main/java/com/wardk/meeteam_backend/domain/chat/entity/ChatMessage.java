package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
    @Index(name = "ix_msg_thread_id_id", columnList = "thread_id, id")
})

/**
 * ChatMessage 엔티티는 채팅 메시지의 세부 정보를 나타내며, 각 메시지는 특정 채팅 스레드에 속합니다.
 * 메시지의 발신자 역할(사용자 또는 시스템), 내용, 사용된 모델 정보 및 토큰 사용량을 포함합니다.
 * 메시지는 BaseEntity를 상속받아 생성 및 수정 시간을 자동으로 관리합니다.
 */
public class ChatMessage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long threadId;

  @Column(nullable = false)
  private Long memberId; // USER 메시지일 때 사용

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SenderRole senderRole;

  @Column(columnDefinition = "text")
  private String content;

  private String modelName;
  private Integer promptTokens;
  private Integer completionTokens;
}
