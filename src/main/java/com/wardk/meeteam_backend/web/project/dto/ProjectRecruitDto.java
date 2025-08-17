package com.wardk.meeteam_backend.web.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectRecruitDto {

    @NotBlank(message = "소분류 이름은 필수입니다.")
    private String subCategory;

    @NotNull(message = "모집 인원은 필수입니다.")
    @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.")
    private Integer recruitmentCount;
}
