package com.wardk.meeteam_backend.web.mainpage.dto.response;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentInfoResponse { // 모집 현황 정보
    private JobPosition jobPosition;
    private Integer recruitmentCount;
    private Integer currentCount;

    /**
     * RecruitmentState를 RecruitmentInfoResponse로 변환하는 정적 메서드
     */
    public static RecruitmentInfoResponse responseDto(RecruitmentState recruitment) {
        return RecruitmentInfoResponse.builder()
                .jobPosition(recruitment.getJobPosition())
                .recruitmentCount(recruitment.getRecruitmentCount())
                .currentCount(recruitment.getCurrentCount())
                .build();
    }
}