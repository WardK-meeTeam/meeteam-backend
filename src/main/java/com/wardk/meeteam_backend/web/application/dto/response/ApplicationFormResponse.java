package com.wardk.meeteam_backend.web.application.dto.response;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 프로젝트 지원서 폼 응답 DTO.
 * 지원자 정보와 지원 가능한 모집 포지션 목록을 포함합니다.
 */
@Schema(description = "프로젝트 지원서 폼 정보")
public record ApplicationFormResponse(
        @Schema(description = "프로젝트 ID")
        Long projectId,

        @Schema(description = "프로젝트명")
        String projectName,

        @Schema(description = "지원자 정보")
        ApplicantInfo applicant,

        @Schema(description = "지원 가능한 모집 포지션 목록")
        List<RecruitmentPositionInfo> recruitmentPositions
) {
    @Schema(description = "지원자 정보")
    public record ApplicantInfo(
            @Schema(description = "회원 ID")
            Long memberId,

            @Schema(description = "이름")
            String name,

            @Schema(description = "나이")
            Integer age,

            @Schema(description = "성별")
            Gender gender,

            @Schema(description = "이메일")
            String email,

            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl
    ) {
        public static ApplicantInfo from(Member member) {
            return new ApplicantInfo(
                    member.getId(),
                    member.getRealName(),
                    member.getAge(),
                    member.getGender(),
                    member.getEmail(),
                    member.getStoreFileName()
            );
        }
    }

    @Schema(description = "모집 포지션 정보")
    public record RecruitmentPositionInfo(
            @Schema(description = "모집 상태 ID")
            Long recruitmentId,

            @Schema(description = "직군 ID")
            Long jobFieldId,

            @Schema(description = "직군명")
            String jobFieldName,

            @Schema(description = "포지션 ID")
            Long jobPositionId,

            @Schema(description = "포지션명")
            String jobPositionName,

            @Schema(description = "모집 인원")
            int recruitCount,

            @Schema(description = "현재 인원")
            int currentCount,

            @Schema(description = "지원 가능 여부")
            boolean available
    ) {
        public static RecruitmentPositionInfo from(RecruitmentState recruitment) {
            boolean available = recruitment.getCurrentCount() < recruitment.getRecruitmentCount();
            return new RecruitmentPositionInfo(
                    recruitment.getId(),
                    recruitment.getJobPosition().getJobField().getId(),
                    recruitment.getJobPosition().getJobField().getName(),
                    recruitment.getJobPosition().getId(),
                    recruitment.getJobPosition().getName(),
                    recruitment.getRecruitmentCount(),
                    recruitment.getCurrentCount(),
                    available
            );
        }
    }

    public static ApplicationFormResponse of(Long projectId, String projectName, Member applicant, List<RecruitmentState> recruitments) {
        List<RecruitmentPositionInfo> positionInfos = recruitments.stream()
                .map(RecruitmentPositionInfo::from)
                .toList();

        return new ApplicationFormResponse(
                projectId,
                projectName,
                ApplicantInfo.from(applicant),
                positionInfos
        );
    }
}
