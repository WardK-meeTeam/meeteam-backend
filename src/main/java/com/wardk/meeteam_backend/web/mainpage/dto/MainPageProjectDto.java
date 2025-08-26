package com.wardk.meeteam_backend.web.mainpage.dto;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainPageProjectDto { // 프로젝트 카드 정보
    private String id; // JavaScript 정밀도 문제 해결을 위해 String 사용
    private String name;
    private String description;
    private String projectCategory;
    private String platformCategory;
    private String imageUrl;
    private String creatorName;
    private LocalDateTime createdDate;
    private LocalDate endDate;
    private List<String> skillNames;
    private List<RecruitmentInfoDto> recruitmentInfo;

    public static MainPageProjectDto responseDto(Project project) {
        return MainPageProjectDto.builder()
                .id(String.valueOf(project.getId()))
                .name(project.getName())
                .description(project.getDescription())
                .projectCategory(project.getProjectCategory().name())
                .platformCategory(project.getPlatformCategory().name())
                .imageUrl(project.getImageUrl())
                .creatorName(project.getCreator().getRealName())
                .createdDate(project.getCreatedAt())
                .endDate(project.getEndDate())
                .skillNames(
                        project.getProjectSkills() != null
                                ? project.getProjectSkills().stream()
                                .map(ps -> ps.getSkill().getSkillName())
                                .collect(Collectors.toList())
                                : List.of()
                )
                .recruitmentInfo(
                        project.getRecruitments() != null
                                ? project.getRecruitments().stream()
                                .map(RecruitmentInfoDto::responseDto)
                                .collect(Collectors.toList())
                                : List.of()
                )
                .build();
    }
}