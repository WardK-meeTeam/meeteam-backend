package com.wardk.meeteam_backend.web.member.dto.response;

import com.wardk.meeteam_backend.domain.job.JobPosition;
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


    private List<CategoryResponse> categories;

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
        this.categories = member.getJobPositions().stream()
                .map(memberJobPosition -> {
                    JobPosition jobPosition = memberJobPosition.getJobPosition();
                    return new CategoryResponse(
                            jobPosition.getJobField(),
                            jobPosition
                    );
                })
                .toList();
        this.skills = member.getMemberSkills().stream()
                .map(memberSkill -> memberSkill.getSkill().getSkillName())
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
        // profileImageUrl과 profileImageName은 Service에서 별도로 설정됨
    }
}