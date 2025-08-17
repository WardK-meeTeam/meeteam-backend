package com.wardk.meeteam_backend.web.member.dto;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.review.entity.Review;
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
//        this.reviewCount
        this.introduce = member.getIntroduction();
//        this.reviewList = reviewList;
        this.projectList = member.getProjectMembers().stream()
                .map(projectMember -> {
                    Project project = projectMember.getProject();
                    return new ProjectResponse(project.getEndDate(),project.getName(), project.getStatus());
                })
                .toList();
    }
}
