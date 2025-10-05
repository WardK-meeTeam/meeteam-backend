package com.wardk.meeteam_backend.web.auth.dto.register;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "소분류 항목")
public class SubCategoryDto {

    @Schema(description = "소분류명", example = "웹서버")
    private String subcategory;
}
