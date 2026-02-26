package com.wardk.meeteam_backend.web.member.dto.response;

import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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

    @Schema(description = "프로젝트 경험 횟수")
    private int projectExperienceCount;

    @Schema(description = "대표 포지션명", example = "웹프론트엔드")
    private String representativePosition;

    private List<GroupedSkillResponse> groupedSkills;
    private List<String> skills;

    private Boolean isParticipating;
    private int projectCount;
    private String introduce;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "프로필 이미지 파일명")
    private String profileImageName;

    private List<MemberProjectResponse> projectList;

    public MemberProfileResponse(Member member, Long memberId) {
        this.memberId = memberId;
        this.name = member.getRealName();
        this.birthDate = member.getBirth();
        this.gender = member.getGender();
        this.email = member.getEmail();
        this.githubUrl = member.getGithubUrl();
        this.blogUrl = member.getBlogUrl();
        this.projectExperienceCount = member.getProjectExperienceCount();

        // 대표 포지션 (첫 번째 직무 포지션)
        this.representativePosition = member.getJobPositions().isEmpty()
            ? null
            : member.getJobPositions().get(0).getJobPosition().getName();

        // 분야별 기술스택 그룹핑
        this.groupedSkills = member.getJobPositions().stream()
            .map(mjp -> {
                JobPosition jp = mjp.getJobPosition();
                List<String> techStacks = member.getMemberTechStacks().stream()
                    .map(mts -> mts.getTechStack().getName())
                    .toList();
                return GroupedSkillResponse.builder()
                    .jobFieldName(jp.getJobField().getName())
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
        this.projectList = member.getProjectMembers().stream()
                .map(projectMember -> projectMember.getProject())
                .filter(project -> !project.isDeleted())
                .map(project -> new MemberProjectResponse(
                        project.getId(),
                        project.getEndDate(),
                        project.getName(),
                        project.getImageUrl(),
                        project.getRecruitmentStatus()
                ))
                .toList();
    }
}
