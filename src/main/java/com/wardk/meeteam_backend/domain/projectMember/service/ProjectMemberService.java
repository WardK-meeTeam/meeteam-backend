package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.JobType;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.projectMember.dto.*;

import java.util.List;

public interface ProjectMemberService {


    public void addCreator(Long projectId, Long memberId, SubCategory subCategory);
    public void addMember(Long projectId, Long memberId, SubCategory subCategory);
    public DeleteResponse deleteProjectMember(DeleteRequest request, String requesterEmail);
    public WithdrawResponse withdraw(WithdrawRequest request, String requesterEmail);
}
