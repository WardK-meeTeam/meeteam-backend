package com.wardk.meeteam_backend.web.member.dto.response;

import com.wardk.meeteam_backend.domain.job.JobField;
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
    private Long projectCount;
    private List<String> skills;
    private List<JobField> jobFields;


    public static MemberCardResponse responseToDto(Member member) {
        return MemberCardResponse.builder()
                .memberId(member.getId())
                .realName(member.getRealName())
                .storeFileName(member.getStoreFileName())
                .projectCount((long) member.getProjectMembers().size())
                .skills(member.getMemberSkills().stream()
                        .map(memberSkill -> memberSkill.getSkill().getSkillName())
                        .toList())
                .jobFields(member.getJobPositions().stream()
                        .map(mjp -> mjp.getJobPosition().getJobField())
                        .distinct()
                        .toList())
                .build();
    }
}