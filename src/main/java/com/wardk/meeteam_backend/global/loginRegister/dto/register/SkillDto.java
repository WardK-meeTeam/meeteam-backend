package com.wardk.meeteam_backend.global.loginRegister.dto.register;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "기술스택 항목")
@Data
public class SkillDto {

    @Schema(description = "스킬명", example = "MySQL")
    private String skillName;


}
