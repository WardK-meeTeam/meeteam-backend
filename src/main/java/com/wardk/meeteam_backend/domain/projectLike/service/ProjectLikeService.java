package com.wardk.meeteam_backend.domain.projectLike.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectLike.entity.ProjectLike;
import com.wardk.meeteam_backend.domain.projectLike.repository.ProjectLikeRepository;
import com.wardk.meeteam_backend.global.aop.Retry;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.projectLike.dto.ToggleLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectLikeService {

    private final ProjectRepository projectRepository;
    private final ProjectLikeRepository projectLikeRepository;
    private final MemberRepository memberRepository;


    /**
     * 좋아요 토글
     */
    @Retry
    @Transactional
    public ToggleLikeResponse toggle(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // userName → memberId 해석 (JWT에서 전달된 userName 기준)
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long memberId = member.getId();

        // 1) 이미 눌렀으면 삭제 먼저 (영향 행 수로 판별)
        long deleted = projectLikeRepository.deleteByMemberIdAndProjectId(memberId, projectId);
        if (deleted > 0) {
            project.decreaseLike(); // @Version 충돌 시 AOP 재시도
            return new ToggleLikeResponse(projectId, false, project.getLikeCount());
        }

        // 2) 없으면 삽입 시도 (동시성에서 유니크 제약 위반 가능)
        try {
            ProjectLike newLike = ProjectLike.create(member, project); // 이미 조회된 영속 엔티티 재사용
            projectLikeRepository.save(newLike);
            project.increaseLike();
            return new ToggleLikeResponse(projectId, true, project.getLikeCount());
        } catch (DataIntegrityViolationException e) {
            // 동시 삽입 레이스: 다른 트랜잭션이 먼저 INSERT 했다면 여기서 제약 위반 발생 가능 → 최종 상태는 '좋아요 ON'
            throw new CustomException(ErrorCode.DB_LIKES_DUPLICATE);
        }
    }







}
