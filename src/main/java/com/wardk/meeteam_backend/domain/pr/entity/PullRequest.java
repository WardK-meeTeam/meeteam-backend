package com.wardk.meeteam_backend.domain.pr.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@Getter
@NoArgsConstructor
@Table(
        name = "pull_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_repo_pr_number",
                        columnNames = {"project_repo_id", "pr_number"}
                )
        }
)
public class PullRequest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  /**
   * PK
   */
  private Long id;

  /**
   * GitHub 저장소의 전체 이름 (예: "owner/repo")
   * projectRepo.getRepoFullName()으로 대체
   */
//  @Column(name = "repo_full_name", nullable = false)
//  private String repoFullName;

  /**
   * PR 번호
   */
  @Column(nullable = false)
  private Integer prNumber;

  /**
   * pr 제목
   */
  private String title;

  /**
   * pr 본문
   */
  @Column(columnDefinition = "TEXT")
  private String body;

  /**
   * pr 상태 (opened, closed 등)
   */
  private String state;

  /**
   * draft pr 여부
   */
  @Column(name = "is_draft")
  private boolean draft;

  /**
   * pr 머지 여부
   */
  @Column(name = "is_merged")
  private boolean merged;

  /**
   * pr 닫힌 시각
   */
  private LocalDateTime closedAt;

  /**
   * pr 머지 시각
   */
  private LocalDateTime mergedAt;

  /**
   * pr 작성자 (깃허브 username)
   */
  private String authorLogin;

  private String baseRepo;
  private String baseBranch;
  private String headRepo;
  private String headBranch;
  private String headSha;

  /**
   * 전체 추가된 라인 수
   */
  private Integer additions;

  /**
   * 전체 삭제된 라인 수
   */
  private Integer deletions;

  /**
   * 변경된 파일 개수
   */
  private Integer changedFiles;

  /**
   * 일반 코멘트 개수
   */
  private Integer commentsCount;
  /**
   * 리뷰 코멘트 개수
   */
  private Integer reviewCommentsCount;

  /**
   * FK, 프로젝트_레포
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_repo_id", nullable = false)
  private ProjectRepo projectRepo;

  /**
   * 연관된 pr 파일 목록
   */
  @OneToMany(mappedBy = "pullRequest", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PullRequestFile> files = new ArrayList<>();

  public void addFile(PullRequestFile file) {
    files.add(file);
    file.setPullRequest(this);
  }

  public PullRequest(Integer prNumber) {
    this.prNumber = prNumber;
  }

  /**
   * github webhook payload에서 필드 매핑
   */
  public void updateFromPayload(JsonNode prNode) {
    this.title = prNode.path("title").asText(null);
    this.body = prNode.path("body").asText(null);
    this.state = prNode.path("state").asText(null);
    this.draft = prNode.path("draft").asBoolean(false);
    this.merged = prNode.path("merged").asBoolean(false);

    this.mergedAt = parseDate(prNode, "merged_at");
    this.closedAt = parseDate(prNode, "closed_at");

    this.authorLogin = prNode.path("user").path("login").asText(null);

    this.baseRepo = prNode.path("base").path("repo").path("full_name").asText(null);
    this.baseBranch = prNode.path("base").path("ref").asText(null);

    this.headRepo = prNode.path("head").path("repo").path("full_name").asText(null);
    this.headBranch = prNode.path("head").path("ref").asText(null);
    this.headSha = prNode.path("head").path("sha").asText(null);

    this.additions = prNode.path("additions").asInt(0);
    this.deletions = prNode.path("deletions").asInt(0);
    this.changedFiles = prNode.path("changed_files").asInt(0);
    this.commentsCount = prNode.path("comments").asInt(0);
    this.reviewCommentsCount = prNode.path("review_comments").asInt(0);
  }

  public void setProjectRepo(ProjectRepo projectRepo) {
    this.projectRepo = projectRepo;
  }

  private LocalDateTime parseDate(JsonNode node, String field) {
    String val = node.path(field).asText(null);
    if(val == null) return null;

    return OffsetDateTime.parse(val, DateTimeFormatter.ISO_DATE_TIME)
            .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
            .toLocalDateTime();
  }
}






