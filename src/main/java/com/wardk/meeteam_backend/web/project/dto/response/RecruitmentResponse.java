package com.wardk.meeteam_backend.web.project.dto.response;

import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.job.JobField;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitmentResponse {

    private JobField jobField;
    private JobPosition jobPosition;
    private int recruitmentCount;
    private int currentCount;
    private boolean isClosed;

    public static RecruitmentResponse responseDto(RecruitmentState recruitment) {
        JobPosition position = recruitment.getJobPosition();
        return RecruitmentResponse.builder()
                .jobField(position.getJobField())
                .jobPosition(position)
                .recruitmentCount(recruitment.getRecruitmentCount())
                .currentCount(recruitment.getCurrentCount())
                .isClosed(recruitment.getCurrentCount() >= recruitment.getRecruitmentCount())
                .build();
    }
}