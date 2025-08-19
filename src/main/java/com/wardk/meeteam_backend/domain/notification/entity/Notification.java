package com.wardk.meeteam_backend.domain.notification.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant; import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Member member;

  private String type; private String title;
  @Column(length = 1000) private String body;
  private String link;

  private boolean isRead = false;
  private Instant createdAt;
}
