
package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.web.project.dto.*;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectUpdateResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {

    ProjectPostResponse postProject(ProjectPostRequest projectPostRequest, MultipartFile file, String requesterEmail);
    List<ProjectListResponse> getProjectList();
    ProjectResponse getProject(Long projectId);
    ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail);
    ProjectDeleteResponse deleteProject(Long projectId, String requesterEmail);
    Slice<ProjectSearchResponse> searchProject(ProjectSearchCondition condition, Pageable pageable);
}

