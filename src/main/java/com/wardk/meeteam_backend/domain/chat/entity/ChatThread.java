package com.wardk.meeteam_backend.domain.chat.entity;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * ChatThread 엔티티는 각 PullRequest에 대해 하나의 채팅 스레드를 나타냅니다.
 * 각 채팅 스레드는 고유한 제목을 가질 수 있으며, 특정 PullRequest와 1:1로 매핑됩니다.
 * 이를 통해 사용자는 각 PullRequest에 대해 별도의 채팅 공간을 가질 수 있습니다.
 * ChatThread는 BaseEntity를 상속받아 생성 및 수정 시간을 자동으로 관리합니다.
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "chat_thread",
    uniqueConstraints = @UniqueConstraint(name = "ux_thread_pr", columnNames = {"pr_id"})
)
public class ChatThread extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pr_id", nullable = false)
  private PullRequest pullRequest;

  @Column(length = 255)
  private String title;
}
