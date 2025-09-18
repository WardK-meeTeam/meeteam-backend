package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import com.wardk.meeteam_backend.domain.projectMember.entity.WeekDay;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ApplicationDetailResponse {

    private Long applicationId;
    private Long applicantId;
    private String applicantName;
    private String subCategoryName; // 피그마엔 대분류 같긴 한데...
    private Integer age;
    private Gender gender;
    private String applicantEmail;
    private String motivation;
    private int availableHoursPerWeek;
    private List<WeekDay> weekDay;
    private boolean offlineAvailable;

    public static ApplicationDetailResponse responseDto(ProjectMemberApplication application) {

        return ApplicationDetailResponse.builder()
                .applicationId(application.getId())
                .applicantId(application.getApplicant().getId())
                .applicantName(application.getApplicant().getRealName())
                .subCategoryName(application.getSubCategory().getName())
                .age(application.getApplicant().getAge())
                .gender(application.getApplicant().getGender())
                .applicantEmail(application.getApplicant().getEmail())
                .motivation(application.getMotivation())
                .availableHoursPerWeek(application.getAvailableHoursPerWeek())
                .weekDay(application.getWeekDays())
                .offlineAvailable(application.isOfflineAvailable())
                .build();
    }
}
