package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.web.project.dto.ProjectPostRequsetDto;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface ProjectService {

    ProjectPostResponseDto postProject(ProjectPostRequsetDto projectPostRequsetDto, MultipartFile file, String requesterEmail);

}
