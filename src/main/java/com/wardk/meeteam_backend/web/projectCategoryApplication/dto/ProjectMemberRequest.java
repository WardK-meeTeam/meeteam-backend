package com.wardk.meeteam_backend.web.projectCategoryApplication.dto;

import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;

public class ProjectMemberRequest {

    private String name;
    private Long memberId;
    private String imageUrl;


    public ProjectMemberRequest(ProjectMember projectMember) {
        this.name = projectMember.getMember().getRealName();
        this.memberId = projectMember.getMember().getId();
        this.imageUrl = projectMember.getMember().getStoreFileName();
    }
}
