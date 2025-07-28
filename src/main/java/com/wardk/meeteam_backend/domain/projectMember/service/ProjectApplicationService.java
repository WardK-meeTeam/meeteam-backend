package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationDecisionRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationDecisionResponse;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationResponse;

public interface ProjectApplicationService {

    public ApplicationResponse apply(ApplicationRequest request, String applicantEmail);
    public ApplicationDecisionResponse decide(ApplicationDecisionRequest request, String requesterEmail);
}
