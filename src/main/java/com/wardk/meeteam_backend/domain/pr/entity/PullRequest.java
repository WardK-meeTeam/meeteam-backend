package com.wardk.meeteam_backend.domain.pr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant; import java.util.UUID;

@Entity
@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
public class PullRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String repoFullName; // org/repo

  @Column(nullable = false)
  private Integer prNumber;

  private String title;

  @Column(length = 2000)
  private String body;

  private String authorLogin;

  private String headRef;
  private String baseRef;
  private String headSha;

  private String state; // OPEN/MERGED/CLOSED
  private Integer changedFiles;
  private Integer additions;
  private Integer deletions;

  private Instant lastSyncedAt;

  // TODO: Project 연관관계는 추후 필요 시 추가(간소화 스켈레톤)
}
