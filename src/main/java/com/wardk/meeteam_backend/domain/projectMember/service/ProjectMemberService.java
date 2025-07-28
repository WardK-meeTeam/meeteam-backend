package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import com.wardk.meeteam_backend.web.projectMember.dto.*;

import java.util.List;

public interface ProjectMemberService {

    public void addMember(Long projectId, Long memberId, JobType jobType);
    public List<ProjectMemberListResponse> getProjectMembers(Long projectId);
    public DeleteResponse deleteProjectMember(DeleteRequest request, String requesterEmail);
    public RoleUpdateResponse updateRole(RoleUpdateRequest request, String requesterEmail);
    public UpdateOwnerResponse updateOwner(UpdateOwnerRequest request, String requesterEmail);
    public WithdrawResponse withdraw(WithdrawRequest request, String requesterEmail);
}
