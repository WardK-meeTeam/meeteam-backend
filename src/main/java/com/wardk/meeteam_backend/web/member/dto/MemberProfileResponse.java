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

    private Long memberId;

    private int age;

    private Gender gender;

    private String email;


    private List<CategoryResponse> categories;

    private List<String> skills;


    private Boolean isParticipating;

    private int projectCount;

    //private int reviewCount;

    private String introduce;

    private int recommendCount;

    private double temperature;

    //private List<ReviewResponse> reviewList;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "프로필 이미지 파일명")
    private String profileImageName;


    private List<ProjectResponse> projectList;


    public MemberProfileResponse(Member member, Long memberId) {
        this.memberId = memberId;
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
                .map(memberSkill -> memberSkill.getSkill().getSkillName())
                .toList();
        this.isParticipating = member.getIsParticipating();
        this.projectCount = member.getProjectMembers().size();
        this.introduce = member.getIntroduction();
        this.recommendCount = member.getRecommendCount();
        this.temperature = member.getTemperature();
        this.projectList = member.getProjectMembers().stream()
                .map(projectMember -> projectMember.getProject())
                .filter(project -> !project.isDeleted())
                .map(project -> new ProjectResponse(
                        project.getId(),
                        project.getEndDate(),
                        project.getName(),
                        project.getImageUrl(),
                        project.getStatus()
                ))
                .toList();
        // profileImageUrl과 profileImageName은 Service에서 별도로 설정됨
    }
}
