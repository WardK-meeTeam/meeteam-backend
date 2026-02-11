package com.wardk.meeteam_backend.domain.member.entity;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.skill.entity.MemberSkill;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthRegisterInfo;
import com.wardk.meeteam_backend.global.auth.service.dto.RegisterMemberCommand;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterRequest;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private String storeFileName;

    @Enumerated(value = EnumType.STRING)
    private Gender gender;

    private LocalDate birth;

    private Boolean isParticipating;

    @Version
    private Long version;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 500)
    private List<MemberJobPosition> jobPositions = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 500)
    private List<MemberSkill> memberSkills = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    @BatchSize(size = 500)
    private List<ProjectMember> projectMembers = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private String provider;

    private String providerId;

    @Column(length = 2048)
    private String oauthAccessToken;

    private Integer projectExperienceCount;

    private String githubUrl;

    private String blogUrl;


    @Builder(access = AccessLevel.PRIVATE)
    private Member(String email, Integer age, String password, String realName,
                   String storeFileName, Gender gender, LocalDate birth,
                   Boolean isParticipating, UserRole role, String provider,
                   String providerId, Integer projectExperienceCount,
                   String githubUrl, String blogUrl) {
        this.email = email;
        this.age = age;
        this.password = password;
        this.realName = realName;
        this.storeFileName = storeFileName;
        this.gender = gender;
        this.birth = birth;
        this.isParticipating = isParticipating;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.projectExperienceCount = projectExperienceCount != null ? projectExperienceCount : 0;
        this.githubUrl = githubUrl;
        this.blogUrl = blogUrl;
    }

    /**
     * 일반 회원가입용 정적 팩토리 메서드
     */
    public static Member createMember(RegisterMemberCommand command, String encodedPassword, String imageUrl ) {
        return Member.builder()
                .email(command.email())
                .password(encodedPassword)
                .realName(command.name())
                .birth(command.birthDate())
                .gender(command.gender())
                .storeFileName(imageUrl)
                .isParticipating(true)
                .role(UserRole.USER)
                .projectExperienceCount(command.projectExperienceCount())
                .githubUrl(command.githubUrl())
                .blogUrl(command.blogUrl())
                .build();
    }

    /**
     * OAuth 회원가입용 정적 팩토리 메서드
     */
    public static Member createOAuthMember(OAuth2RegisterRequest request, OAuthRegisterInfo registerInfo,
                                            String encodedPassword, String imageUrl) {
        return Member.builder()
                .email(registerInfo.getEmail())
                .password(encodedPassword)
                .realName(request.getName())
                .birth(request.getBirthDate())
                .gender(request.getGender())
                .storeFileName(imageUrl)
                .isParticipating(true)
                .role(UserRole.USER)
                .provider(registerInfo.getProvider())
                .providerId(registerInfo.getProviderId())
                .projectExperienceCount(request.getProjectExperienceCount())
                .githubUrl(request.getGithubUrl())
                .blogUrl(request.getBlogUrl())
                .build();
    }

    /**
     * OAuth2 인증 과정에서 임시 회원 생성용 (DB 저장 전)
     */
    public static Member createOAuth2Guest(String email, String realName, String provider, String providerId) {
        return Member.builder()
                .email(email)
                .realName(realName)
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.OAUTH2_GUEST)
                .build();
    }

    /**
     * 테스트/시드 데이터용 정적 팩토리 메서드
     */
    public static Member createForTest(String email, String realName) {
        return Member.builder()
                .email(email)
                .realName(realName)
                .role(UserRole.USER)
                .build();
    }

    /**
     * 테스트용 일반 회원 생성 (비밀번호 포함)
     */
    public static Member createForTest(String email, String realName, String encodedPassword) {
        return Member.builder()
                .email(email)
                .realName(realName)
                .password(encodedPassword)
                .birth(java.time.LocalDate.of(1998, 3, 15))
                .gender(Gender.MALE)
                .isParticipating(true)
                .role(UserRole.USER)
                .build();
    }

    /**
     * 테스트용 OAuth 회원 생성
     */
    public static Member createOAuthForTest(String email, String realName, String provider, String providerId) {
        return Member.builder()
                .email(email)
                .realName(realName)
                .birth(java.time.LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .isParticipating(true)
                .role(UserRole.USER)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    public void addJobPosition(JobPosition jobPosition) {
        jobPositions.add(new MemberJobPosition(this, jobPosition));
    }

    public void addMemberSkill(Skill skill) {
        memberSkills.add(new MemberSkill(this, skill));
    }

    /**
     * 회원가입 시 직무와 스킬을 일괄 초기화
     */
    public void initializeDetails(List<JobPosition> jobPositions, List<Skill> skills) {
        jobPositions.forEach(jobPosition -> addJobPosition(jobPosition));
        skills.forEach(skill -> addMemberSkill(skill));
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public void setOauthAccessToken(String oauthAccessToken) {
        this.oauthAccessToken = oauthAccessToken;
    }

    public void setStoreFileName(String storeFileName) {
        this.storeFileName = storeFileName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setIsParticipating(Boolean isParticipating) {
        this.isParticipating = isParticipating;
    }
}