package com.wardk.meeteam_backend.domain.member.entity;


import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.skill.entity.MemberSkill;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@Getter
@Setter
@Builder
@NoArgsConstructor
@Table(name = "member", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Member extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private Integer age;

    private String password;

    private String realName;

    //image_URL
    private String storeFileName;

    @Enumerated(value = EnumType.STRING)
    private Gender gender;

    private LocalDate birth;

    private Boolean isParticipating;

    // 추천 받은 횟수
    @Builder.Default
    private int recommendCount = 0;

    @Builder.Default
    private double temperature = 36.5;

    @Version
    private Long version;

    public void increaseRecommendCount() {
        this.recommendCount++;
        this.temperature = 36.5 + recommendCount * 0.1;
    }


    //사용자가 회원가입 할때 넣은 소분류 항목들..
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 500) // N+1 문제 해결을 위한 배치 로딩
    @Builder.Default
    List<MemberSubCategory> subCategories = new ArrayList<>();

    //연관관계 편의메서드
    public void addSubCategory(SubCategory signupSubCategory) {
        subCategories.add(new MemberSubCategory(this, signupSubCategory));
    }

    //사용자 기술 스택
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 500) // N+1 문제 해결을 위한 배치 로딩
    @Builder.Default
    List<MemberSkill> memberSkills = new ArrayList<>();

    //연관관계 편의메서드
    public void addMemberSkill(Skill skill) {
        memberSkills.add(new MemberSkill(this, skill));
    }


    @OneToMany(mappedBy = "member")
    @BatchSize(size = 500) // N+1 문제 해결을 위한 배치 로딩
    @Builder.Default
    List<ProjectMember> projectMembers = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String introduction;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // 소셜 로그인 관련 필드 추가
    private String provider; // 소셜 로그인 제공자 (예: "google", "github" 등)
    private String providerId; // 소셜 로그인 한 유저의 고유 ID가 들어감

    @Column(length = 2048)
    private String oauthAccessToken; // OAuth 제공자의 Access Token (로그아웃 시 토큰 철회용)



    public Member(String email, Integer age, String password, String realName, String storeFileName, Gender gender, LocalDate birth) {
        this.email = email;
        this.age = age;
        this.password = password;
        this.realName = realName;
        this.storeFileName = storeFileName;
        this.gender = gender;
        this.birth = birth;
    }
}
