package com.wardk.meeteam_backend.web.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProjectRepoRequest {

    private List<String> repoFullNames;

}
