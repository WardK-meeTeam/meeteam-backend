package com.wardk.meeteam_backend.domain.codereview.repository;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * PR 리뷰 작업 레포지토리
 */
public interface PrReviewJobRepository extends JpaRepository<PrReviewJob, Long> {
    
    @Query("""
        SELECT j 
        FROM PrReviewJob j 
        JOIN FETCH j.pullRequest 
        WHERE j.id = :id
        """)
    Optional<PrReviewJob> findByIdWithPullRequest(@Param("id") Long id);

    @Query("""
        SELECT j 
        FROM PrReviewJob j 
        JOIN FETCH j.pullRequest pr
        JOIN FETCH pr.projectRepo repo
        JOIN FETCH pr.files
        WHERE j.id = :id
        """)
    Optional<PrReviewJob> findByIdWithAllAssociations(@Param("id") Long id);

    /**
     * 저장소 ID, PR 번호, HEAD SHA로 리뷰 작업을 조회
     */
    @Query("""
        SELECT j 
        FROM PrReviewJob j 
        JOIN FETCH j.pullRequest pr
        JOIN FETCH pr.projectRepo repo
        WHERE pr.projectRepo.id = :repoId 
        AND pr.prNumber = :prNumber 
        AND j.headSha = :headSha
        """)
    Optional<PrReviewJob> findByProjectRepoIdAndPrNumberAndHeadSha(
            @Param("repoId") Long repoId, 
            @Param("prNumber") Integer prNumber, 
            @Param("headSha") String headSha);

    /**
     * PR로 리뷰 작업 목록을 조회
     */
    List<PrReviewJob> findByPullRequest(PullRequest pullRequest);

    /**
     * 저장소 ID와 PR 번호로 가장 최근 리뷰 작업을 조회
     */
    @Query("""
        SELECT j 
        FROM PrReviewJob j 
        JOIN j.pullRequest pr
        WHERE pr.projectRepo.id = :repoId 
        AND pr.prNumber = :prNumber
        ORDER BY j.createdAt DESC
        """)
    Optional<PrReviewJob> findLatestByRepoAndPrNumber(
            @Param("repoId") Long repoId, 
            @Param("prNumber") Integer prNumber);

    /**
     * 특정 상태의 모든 리뷰 작업을 조회
     */
    List<PrReviewJob> findByStatus(PrReviewJob.Status status);
}
