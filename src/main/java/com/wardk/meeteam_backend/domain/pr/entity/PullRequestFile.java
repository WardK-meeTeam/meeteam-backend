package com.wardk.meeteam_backend.domain.pr.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
public class PullRequestFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private PullRequest pullRequest;

  @Column(nullable = false)
  private String filename;

  private String status; // modified/added/removed
  private Integer additions;
  private Integer deletions;

  private String patchKey; // 대용량 patch는 ObjectStorage key 참조
}
