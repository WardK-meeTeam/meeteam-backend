package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant; import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pull_request_id"}))
public class ChatThread {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private PullRequest pullRequest;

  @ManyToOne(fetch = FetchType.LAZY) // 생성자(봇/사용자)
  private Member createdBy;

  private Instant createdAt;
  private Instant updatedAt;
}
