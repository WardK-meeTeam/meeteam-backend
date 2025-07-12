package com.wardk.meeteam_backend.domain.member.entity;


import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectMember;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Entity
@AllArgsConstructor
@Builder
@Getter
public class Member extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false)
    private String email;

    private String password;

    private String nickName;

    private String realName;

    private String storeFileName;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    //이 프로젝트는 Member 가 팀장인 경우임(즉 직접 만든 프로젝트) 참여한 프로젝트는 ProjectMember 를 통해서
    @OneToMany(mappedBy = "creator")
    private List<Project> createdProject;

    //연관관계 편의 메서드
    public void createProject(Project project) {
        createdProject.add(project);
        project.setProjectCreator(this);
    }



    public Member() {
    }

    @Builder
    public Member(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Builder
    public Member(String email, String password, String nickName, JobType jobType , String storeFileName) {
        this.email = email;
        this.password = password;
        this.nickName = nickName;
        this.jobType = jobType;
        this.storeFileName = storeFileName;
    }

    public static Member createMember(String email, String password, String nickName, JobType jobType, String storeFileName) {
        return Member.builder()
                .email(email)
                .password(password)
                .nickName(nickName)
                .jobType(jobType)
                .storeFileName(storeFileName)
                .build();
    }
}
