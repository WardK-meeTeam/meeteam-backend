package com.wardk.meeteam_backend.web.member.dto;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MemberCardResponse {

    private Long memberId;
    private String realName;
    private String storeFileName;
    private double temperature;
    private Long projectCount;
    private List<String> skillList;
    private List<String> bigCategory; // 대분류 추가


    public static MemberCardResponse responseToDto(Member member) {
        return MemberCardResponse.builder()
                .memberId(member.getId())
                .realName(member.getRealName())
                .storeFileName(member.getStoreFileName())
                .temperature(member.getTemperature())
                .projectCount((long) member.getProjectMembers().size())
                .skillList(member.getMemberSkills().stream()
                        .map(memberSkill -> memberSkill.getSkill().getSkillName()) // Skill 이름만 추출
                        .toList())
                .bigCategory(member.getSubCategories().stream()
                        .map(memberSubCategory -> memberSubCategory.getSubCategory().getBigCategory().getName())
                        .distinct() // 중복 제거
                        .toList()) // List로 수집
                .build();
    }
}
