package com.wardk.meeteam_backend.web.project.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProjectRepoRequest {

    @NotEmpty
    private List<String> repoUrls;

}
