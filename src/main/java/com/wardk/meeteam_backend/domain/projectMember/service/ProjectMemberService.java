package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.web.projectMember.dto.request.*;
import com.wardk.meeteam_backend.web.projectMember.dto.response.*;

import java.util.List;

public interface ProjectMemberService {

    void addCreator(Long projectId, Long memberId, JobPosition jobPosition);
    void addMember(Long projectId, Long memberId, JobPosition jobPosition);
    List<ProjectMemberListResponse> getProjectMembers(Long projectId);
    DeleteResponse deleteProjectMember(DeleteRequest request, String requesterEmail);
    WithdrawResponse withdraw(WithdrawRequest request, String requesterEmail);
}