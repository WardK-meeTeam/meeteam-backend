package com.wardk.meeteam_backend.acceptance.cucumber.support;

import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectmember.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Repository 통합 진입점 (Facade)
 */
@Component
public class TestRepositorySupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    public MemberRepository member() {
        return memberRepository;
    }

    public ProjectRepository project() {
        return projectRepository;
    }

    public ProjectMemberRepository projectMember() {
        return projectMemberRepository;
    }
}