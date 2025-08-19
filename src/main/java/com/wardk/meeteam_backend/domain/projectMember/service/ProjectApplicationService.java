package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.web.projectMember.dto.*;

import java.util.List;

public interface ProjectApplicationService {

    public ApplicationResponse apply(ApplicationRequest request, String applicantEmail);
    public List<ProjectApplicationListResponse> getApplicationList(Long projectId, String requesterEmail);
    public ApplicationDecisionResponse decide(ApplicationDecisionRequest request, String requesterEmail);
}
