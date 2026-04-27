package com.wardk.meeteam_backend.web.member.dto.response;

import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectCardResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberProfileResponse {


    private String name;
    private Long memberId;

    @Schema(description = "생년월일", example = "1998-03-15")
    private LocalDate birthDate;

    private Gender gender;
    private String email;

    @Schema(description = "GitHub URL")
    private String githubUrl;

    @Schema(description = "블로그 URL")
    private String blogUrl;

    @Schema(description = "대표 포지션 한글명", example = "웹 프론트엔드")
    private String representativePosition;

    @Schema(description = "대표 포지션 영문명", example = "Frontend Dev")
    private String representativePositionEn;

    private List<GroupedSkillResponse> groupedSkills;
    private List<String> skills;

    private Boolean isParticipating;
    private int projectCount;
    private String introduce;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "프로필 이미지 파일명")
    private String profileImageName;

    @Schema(description = "참여 프로젝트 카드 목록")
    private List<ProjectCardResponse> projectCards;

    public MemberProfileResponse(Member member, Long memberId) {
        this.memberId = memberId;
        this.name = member.getRealName();
        this.birthDate = member.getBirth();
        this.gender = member.getGender();
        this.email = member.getEmail();
        this.githubUrl = member.getGithubUrl();
        this.blogUrl = member.getBlogUrl();

        // 대표 포지션 (첫 번째 직무 포지션)
        if (!member.getJobPositions().isEmpty()) {
            JobPosition firstPosition = member.getJobPositions().get(0).getJobPosition();
            this.representativePosition = firstPosition.getName();
            this.representativePositionEn = firstPosition.getCode().getEnglishName();
        }

        // 분야별 기술스택 그룹핑 (JobField 기준으로 필터링)
        this.groupedSkills = member.getJobPositions().stream()
            .map(mjp -> {
                JobPosition jp = mjp.getJobPosition();
                JobField jf = jp.getJobField();

                // 해당 직군에 속하는 기술스택 ID 목록
                Set<Long> fieldTechStackIds = jf.getJobFieldTechStacks().stream()
                    .map(jfts -> jfts.getTechStack().getId())
                    .collect(Collectors.toSet());

                // 회원의 기술스택 중 해당 직군에 속하는 것만 필터링
                List<String> techStacks = member.getMemberTechStacks().stream()
                    .filter(mts -> fieldTechStackIds.contains(mts.getTechStack().getId()))
                    .map(mts -> mts.getTechStack().getName())
                    .toList();

                return GroupedSkillResponse.builder()
                    .jobFieldName(jf.getName())
                    .jobPositionName(jp.getName())
                    .techStacks(techStacks)
                    .build();
            }).toList();

        this.skills = member.getMemberTechStacks().stream()
                .map(memberTechStack -> memberTechStack.getTechStack().getName())
                .toList();
        this.isParticipating = member.getIsParticipating();
        this.projectCount = member.getProjectMembers().size();
        this.introduce = member.getIntroduction();
    }
}
