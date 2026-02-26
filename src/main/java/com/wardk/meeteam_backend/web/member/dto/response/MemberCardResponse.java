package com.wardk.meeteam_backend.web.member.dto.response;

import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MemberCardResponse {

    private Long memberId;
    private String name;
    private String profileImageUrl;
    private String jobFieldName; // 대표 직군 한글명
    private String jobPositionNameEn; // 대표 직무 영문명 (피그마: "Frontend Dev")
    private int projectCount;
    private List<String> mainSkills; // 주요 기술스택 (전체 반환, 프론트에서 + N 처리)


    public static MemberCardResponse responseToDto(Member member) {
        String jobFieldName = member.getJobPositions().isEmpty()
            ? null
            : member.getJobPositions().get(0).getJobPosition().getJobField().getName();

        String jobPositionNameEn = member.getJobPositions().isEmpty()
            ? null
            : member.getJobPositions().get(0).getJobPosition().getCode().getEnglishName();

        List<String> mainSkills = member.getMemberTechStacks().stream()
            .map(mts -> mts.getTechStack().getName())
            .limit(3)
            .toList();

        return MemberCardResponse.builder()
            .memberId(member.getId())
            .name(member.getRealName())
            .profileImageUrl(member.getStoreFileName())
            .jobFieldName(jobFieldName)
            .jobPositionNameEn(jobPositionNameEn)
            .projectCount(member.getProjectExperienceCount())
            .mainSkills(mainSkills)
            .build();
    }
}
