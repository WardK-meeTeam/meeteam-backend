package com.wardk.meeteam_backend.domain.pr.entity;

import com.wardk.meeteam_backend.global.entity.BaseEntity;
import com.wardk.meeteam_backend.web.pr.dto.PrFileData;
import jakarta.persistence.*;
import lombok.*;


@Entity
@AllArgsConstructor
@Getter
@NoArgsConstructor
@Table(name = "pull_request_file")
public class PullRequestFile extends BaseEntity {

  /**
   * PK
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private PullRequest pullRequest;

  /**
   * 파일 경로
   */
  @Column(nullable = false)
  private String fileName;

  /**
   * 파일 상태 (modified, added, removed)
   */
  private String status;

  /**
   * 추가된 라인 수
   */
  private Integer additions;

  /**
   * 삭제된 라인 수
   */
  private Integer deletions;

  /**
   * 변경된 라인 수
   */
    private Integer changes;

  /**
   * 깃허브 blob SHA
   */
  private String blobSha;

  /**
   * rename된 경우 이전 파일명
   */
  private String previousFilename;

  // 파일 크기
  private Integer size;

  /**
   * patch 내용 (임계치 초과 -> null)
   */
  @Column(columnDefinition = "TEXT")
  private String patch;

  /**
   * patch 저장 키
   */
  private String patchKey; // 대용량 patch는 ObjectStorage key 참조

  @Builder
  public PullRequestFile(String filename, String status, Integer additions, Integer deletions, Integer changes, String blobSha, String previousFilename, Integer size, String patch, String patchKey) {
    this.fileName = filename;
    this.status = status;
    this.additions = additions;
    this.deletions = deletions;
    this.changes = changes;
    this.blobSha = blobSha;
    this.previousFilename = previousFilename;
    this.size = size;
    this.patch = patch;
    this.patchKey = patchKey;
  }

  public static PullRequestFile createPullRequestFile(PrFileData prFileData) {

    String patch = prFileData.getPatch();
    String patchKey = null;

    if(patch != null && patch.length() > 10000) {
      //TODO: 외부 스토리지(S3)에 업로드 후 키 반환
      // 임의로 키 생성
      patchKey = "TEMP_KEY_" + prFileData.getFileName() + "_" + System.currentTimeMillis();
      patch = null;
    }

    return PullRequestFile.builder()
            .filename(prFileData.getFileName())
            .status(prFileData.getStatus())
            .additions(prFileData.getAdditions())
            .deletions(prFileData.getDeletions())
            .changes(prFileData.getChanges())
            .blobSha(prFileData.getBlobSha())
            .previousFilename(prFileData.getPreviousFileName())
            .size(prFileData.getSize())
            .patch(patch)
            .patchKey(patchKey)
            .build();
  }

  public void setPullRequest(PullRequest pullRequest) {
    this.pullRequest = pullRequest;
  }
}
