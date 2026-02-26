package com.wardk.meeteam_backend.domain.projectMember.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectMemberRole {
    LEADER("리더"),
    MEMBER("팀원");

    private final String description;
}
