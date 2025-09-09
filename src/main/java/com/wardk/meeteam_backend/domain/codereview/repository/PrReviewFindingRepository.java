package com.wardk.meeteam_backend.domain.codereview.repository;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewFinding;
import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * PR 리뷰 발견 항목 레포지토리
 */
public interface PrReviewFindingRepository extends JpaRepository<PrReviewFinding, Long> {
    
    /**
     * 리뷰 작업에 속한 모든 발견 항목 조회
     */
    List<PrReviewFinding> findByPrReviewJob(PrReviewJob reviewJob);
    
    // /**
    //  * 특정 샤드 ID를 가진 발견 항목 수 조회
    //  */
    // @Query("""
    //     SELECT COUNT(f) 
    //       FROM PrReviewFinding f 
    //      WHERE f.shardId = :shardId
    //      """)
    // int countByShardId(@Param("shardId") String shardId);
    
    // /**
    //  * 파일 경로별 발견 항목 수 집계
    //  */
    // @Query("""
    //     SELECT f.filePath, COUNT(f) 
    //     FROM PrReviewFinding f 
    //     WHERE f.reviewJob = :job 
    //     GROUP BY f.filePaths
    //     """)
    // List<Object> countByFilePathGrouped(@Param("job") PrReviewJob job);
    
    // /**
    //  * 심각도별 발견 항목 수 집계
    //  */
    // @Query("""
    //     SELECT f.severity, COUNT(f) 
    //     FROM PrReviewFinding f 
    //     WHERE f.reviewJob = :job 
    //     GROUP BY f.severity
    //     """)
    // List<Object> countBySeverityGrouped(@Param("job") PrReviewJob job);
}
