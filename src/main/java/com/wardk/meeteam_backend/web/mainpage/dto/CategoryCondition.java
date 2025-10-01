package com.wardk.meeteam_backend.web.mainpage.dto;

import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryCondition {

    private ProjectCategory projectCategory;
}
