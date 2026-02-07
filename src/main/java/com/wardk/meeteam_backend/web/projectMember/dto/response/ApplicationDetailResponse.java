package com.wardk.meeteam_backend.web.projectmember.dto.response;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.projectmember.entity.WeekDay;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ApplicationDetailResponse {

    private Long applicationId;
    private Long applicantId;
    private String applicantName;
    private JobPosition jobPosition;
    private String imageUrl;
    private Integer age;
    private Gender gender;
    private String applicantEmail;
    private String motivation;
    private int availableHoursPerWeek;
    private List<WeekDay> weekDay;
    private boolean offlineAvailable;

    public static ApplicationDetailResponse responseDto(ProjectApplication application) {

        return ApplicationDetailResponse.builder()
                .applicationId(application.getId())
                .applicantId(application.getApplicant().getId())
                .applicantName(application.getApplicant().getRealName())
                .jobPosition(application.getJobPosition())
                .imageUrl(application.getApplicant().getStoreFileName())
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
