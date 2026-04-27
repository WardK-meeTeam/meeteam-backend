package com.wardk.meeteam_backend.web.member.dto.response;

import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectCardResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 특정 사용자 상세 조회 응답 DTO.
 */
@Schema(description = "회원 상세 정보")
@Getter
@Builder
public class MemberDetailResponse {

    @Schema(description = "회원 ID")
    private final Long memberId;

    @Schema(description = "프로필 이미지 URL")
    private final String profileImageUrl;

    @Schema(description = "이름")
    private final String name;

    @Schema(description = "나이", example = "23")
    private final Integer age;

    @Schema(description = "성별")
    private final Gender gender;

    @Schema(description = "대표 포지션 (영문)", example = "Frontend Dev")
    private final String representativePosition;

    @Schema(description = "직군 목록", example = "[\"프론트\", \"웹 프론트엔드\"]")
    private final List<String> jobPositions;

    @Schema(description = "이메일")
    private final String email;

    @Schema(description = "GitHub URL")
    private final String githubUrl;

    @Schema(description = "블로그 URL")
    private final String blogUrl;

    @Schema(description = "프로젝트 참여 희망 여부")
    private final Boolean isParticipating;

    @Schema(description = "자기소개")
    private final String introduce;

    @Schema(description = "참여 프로젝트 수")
    private final Integer participatedProjectCount;

    @Schema(description = "참여 프로젝트 목록")
    private final List<ProjectCardResponse> participatedProjects;

    @Schema(description = "보유 기술 (직군별 그룹)")
    private final List<GroupedSkillInfo> groupedSkills;

    /**
     * 직군별 기술스택 그룹 정보.
     */
    @Schema(description = "직군별 기술스택 그룹")
    @Getter
    @Builder
    public static class GroupedSkillInfo {

        @Schema(description = "직군명", example = "프론트")
        private final String jobFieldName;

        @Schema(description = "포지션명", example = "웹 프론트엔드")
        private final String jobPositionName;

        @Schema(description = "기술스택 목록", example = "[\"React.js\", \"Zustand\", \"SwiftUI\"]")
        private final List<String> techStacks;
    }

    /**
     * Member 엔티티로부터 MemberDetailResponse 생성.
     *
     * @param member             회원 엔티티
     * @param participatedProjects 참여 프로젝트 카드 목록
     */
    public static MemberDetailResponse from(Member member, List<ProjectCardResponse> participatedProjects) {
        // 나이 계산
        Integer age = null;
        if (member.getBirth() != null) {
            age = Period.between(member.getBirth(), LocalDate.now()).getYears();
        }

        // 대표 포지션 (첫 번째 직무 포지션의 영문명)
        String representativePosition = null;
        if (!member.getJobPositions().isEmpty()) {
            JobPosition firstPosition = member.getJobPositions().get(0).getJobPosition();
            representativePosition = firstPosition.getCode().getEnglishName();
        }

        // 직군 목록 (포지션명 리스트)
        List<String> jobPositionNames = member.getJobPositions().stream()
                .map(mjp -> mjp.getJobPosition().getName())
                .toList();

        // 직군별 기술스택 그룹핑
        List<GroupedSkillInfo> groupedSkills = member.getJobPositions().stream()
                .map(mjp -> {
                    JobPosition jp = mjp.getJobPosition();
                    JobField jf = jp.getJobField();

                    // 해당 직군에 속하는 기술스택 ID 목록
                    Set<Long> fieldTechStackIds = jf.getJobFieldTechStacks().stream()
                            .map(jfts -> jfts.getTechStack().getId())
                            .collect(Collectors.toSet());

                    // 회원의 기술스택 중 해당 직군에 속하는 것만 필터링 (displayOrder 순)
                    List<String> techStacks = member.getMemberTechStacks().stream()
                            .filter(mts -> fieldTechStackIds.contains(mts.getTechStack().getId()))
                            .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                            .map(mts -> mts.getTechStack().getName())
                            .toList();

                    return GroupedSkillInfo.builder()
                            .jobFieldName(jf.getName())
                            .jobPositionName(jp.getName())
                            .techStacks(techStacks)
                            .build();
                })
                .toList();

        return MemberDetailResponse.builder()
                .memberId(member.getId())
                .profileImageUrl(member.getStoreFileName())
                .name(member.getRealName())
                .age(age)
                .gender(member.getGender())
                .representativePosition(representativePosition)
                .jobPositions(jobPositionNames)
                .email(member.getEmail())
                .githubUrl(member.getGithubUrl())
                .blogUrl(member.getBlogUrl())
                .isParticipating(member.getIsParticipating())
                .introduce(member.getIntroduction())
                .participatedProjectCount(participatedProjects.size())
                .participatedProjects(participatedProjects)
                .groupedSkills(groupedSkills)
                .build();
    }
}