package com.wardk.meeteam_backend.web.member.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.skill.entity.MemberSkill;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class MemberCardResponse {

    private Long memberId;
    private String realName;
    private String storeFileName;
    private double temperature;
    private Long projectCount;
    private List<String> skillList; // Skill 엔티티 대신 String으로 변경


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
                .build();
    }
}
