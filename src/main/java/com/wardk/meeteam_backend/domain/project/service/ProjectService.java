
package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.web.project.dto.ProjectListResponse;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostRequest;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostResponse;
import com.wardk.meeteam_backend.web.project.dto.ProjectResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {

    ProjectPostResponse postProject(ProjectPostRequest projectPostRequest, MultipartFile file, String requesterEmail);
    List<ProjectListResponse> getProjectList();
    ProjectResponse getProject(Long projectId);
}

