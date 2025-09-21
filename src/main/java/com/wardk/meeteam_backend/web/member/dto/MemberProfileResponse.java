package com.wardk.meeteam_backend.web.member.dto;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.review.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberProfileResponse {


    private String name;
    private int age;

    private Gender gender;

    private String email;


    private List<CategoryResponse> categories;

    private List<SkillResponse> skills;


    private Boolean isParticipating;

    private int projectCount;

    private int reviewCount;

    private String introduce;

    private List<ReviewResponse> reviewList;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "프로필 이미지 파일명")
    private String profileImageName;


    private List<ProjectResponse> projectList;


    public MemberProfileResponse(Member member) {
        this.name = member.getRealName();
        this.age = member.getAge();
        this.gender = member.getGender();
        this.email = member.getEmail();
        this.categories = member.getSubCategories().stream()
                .map(memberSubCategory -> {
                    SubCategory subCategory = memberSubCategory.getSubCategory();
                    return new CategoryResponse(subCategory.getBigCategory().getName(), subCategory.getName());
                })
                .toList();
        this.skills = member.getMemberSkills().stream()
                .map(memberSkill -> {
                    return new SkillResponse(memberSkill.getSkill().getSkillName());
                })
                .toList();
        this.isParticipating = member.getIsParticipating();
        this.projectCount = member.getProjectMembers().size();
        this.introduce = member.getIntroduction();
        this.projectList = member.getProjectMembers().stream()
                .map(projectMember -> {
                    Project project = projectMember.getProject();
                    return new ProjectResponse(project.getId(), project.getEndDate(), project.getName(), project.getStatus());
                })
                .toList();
        // profileImageUrl과 profileImageName은 Service에서 별도로 설정됨
    }
}
