package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;

@Getter
@Setter
@ParameterObject
public class ApplicationRequest {

    @NotNull
    @Schema(description = "프로젝트 ID", example = "1")
    private Long projectId;

    @NotNull
    @Schema(description = "지원 분야", example = "BACKEND")
    private JobType jobType;

    @NotBlank
    @Schema(description = "지원 동기", example = "이 프로젝트에 참여하고 싶습니다.")
    private String motivation;

    @NotNull
    @Schema(description = "주당 투자 가능 시간", example = "10")
    private Integer availableHoursPerWeek;

    @NotEmpty
    @Schema(description = "가능한 요일", example = "[\"월\", \"수\"]")
    private String[] availableDays;

    @NotNull
    @Schema(description = "오프라인 참여 가능 여부", example = "true")
    private Boolean offlineAvailable;
}
