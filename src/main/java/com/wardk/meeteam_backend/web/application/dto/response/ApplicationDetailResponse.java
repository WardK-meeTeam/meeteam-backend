package com.wardk.meeteam_backend.web.application.dto.response;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationAnswer;
import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Comparator;
import java.util.List;

/**
 * 프로젝트 지원 상세 응답 DTO.
 */
@Schema(description = "프로젝트 지원 상세 응답")
public record ApplicationDetailResponse(
        @Schema(description = "지원서 ID")
        Long applicationId,

        @Schema(description = "지원자 ID")
        Long applicantId,

        @Schema(description = "지원자 이름")
        String applicantName,

        @Schema(description = "지원자 프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "지원자 나이")
        Integer age,

        @Schema(description = "지원자 성별")
        Gender gender,

        @Schema(description = "지원자 이메일")
        String applicantEmail,

        @Schema(description = "지원 포지션 정보")
        JobPositionInfo jobPosition,

        @Schema(description = "지원자 기술스택 목록 (displayOrder 순)")
        List<TechStackInfo> techStacks,

        @Schema(description = "지원 사유 및 자기소개")
        String motivation,

        @Schema(description = "동적 지원서 질문/답변 목록")
        List<AnswerInfo> answers,

        @Schema(description = "지원 상태")
        String status
) {
    @Schema(description = "지원서 질문/답변 정보")
    public record AnswerInfo(
            @Schema(description = "질문 ID")
            Long questionId,

            @Schema(description = "질문 문구")
            String label,

            @Schema(description = "입력 타입")
            String inputType,

            @Schema(description = "답변 값")
            String value
    ) {
    }

    @Schema(description = "지원 포지션 정보")
    public record JobPositionInfo(
            @Schema(description = "포지션 ID")
            Long jobPositionId,

            @Schema(description = "포지션명")
            String jobPositionName,

            @Schema(description = "직군 ID")
            Long jobFieldId,

            @Schema(description = "직군명")
            String jobFieldName
    ) {
    }

    @Schema(description = "기술스택 정보")
    public record TechStackInfo(
            @Schema(description = "기술스택 ID")
            Long id,

            @Schema(description = "기술스택명")
            String name,

            @Schema(description = "표시 순서")
            Integer displayOrder
    ) {
    }

    public static ApplicationDetailResponse from(ProjectApplication application, List<ApplicationAnswer> answers) {
        Member applicant = application.getApplicant();

        List<TechStackInfo> techStacks = applicant.getMemberTechStacks().stream()
                .sorted(Comparator.comparing(mts -> mts.getDisplayOrder()))
                .map(mts -> new TechStackInfo(
                        mts.getTechStack().getId(),
                        mts.getTechStack().getName(),
                        mts.getDisplayOrder()
                ))
                .toList();

        List<AnswerInfo> answerInfos = answers.stream()
                .map(answer -> new AnswerInfo(
                        answer.getQuestion().getId(),
                        answer.getQuestion().getLabel(),
                        answer.getQuestion().getInputType().name(),
                        answer.getValue()
                ))
                .toList();

        return new ApplicationDetailResponse(
                application.getId(),
                applicant.getId(),
                applicant.getRealName(),
                applicant.getStoreFileName(),
                applicant.getAge(),
                applicant.getGender(),
                applicant.getEmail(),
                new JobPositionInfo(
                        application.getJobPosition().getId(),
                        application.getJobPosition().getName(),
                        application.getJobPosition().getJobField().getId(),
                        application.getJobPosition().getJobField().getName()
                ),
                techStacks,
                application.getMotivation(),
                answerInfos,
                application.getStatus().getDisplayName()
        );
    }
}
