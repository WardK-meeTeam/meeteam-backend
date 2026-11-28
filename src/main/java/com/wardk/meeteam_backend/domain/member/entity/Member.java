package com.wardk.meeteam_backend.domain.member.entity;

import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.job.entity.TechStack;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMember;
import com.wardk.meeteam_backend.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_email_deleted", columnNames = {"email", "deleted_at"}),
        @UniqueConstraint(name = "uk_member_student_id_deleted", columnNames = {"student_id", "deleted_at"})
},
        // projectCount 정렬 조회(메인페이지, 팀원 검색)의 filesort를 인덱스 순서 스캔으로 대체.
        // LIMIT 페이지가 차면 조기 종료할 수 있는 정렬 인덱스 — deleted_at IS NULL 필터가 선행 컬럼.
        indexes = @Index(
                name = "idx_member_deleted_project_count",
                columnList = "deleted_at, project_count"
        ))
public class Member extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false)
    private String email;

    private Integer age;

    private String password;

    private String realName;

    private String storeFileName;

    @Enumerated(value = EnumType.STRING)
    private Gender gender;

    private LocalDate birth;

    private Boolean isParticipating;

    /**
     * 참여 프로젝트 수(비정규화 캐시 컬럼). 값의 의미 = 회원별 {@code project_member} 행 수(전부 카운트).
     *
     * <p>팀원 찾기 projectCount 정렬의 상관 스칼라 서브쿼리를 컬럼 정렬로 대체하기 위한 컬럼이다.
     * 증감은 ProjectMember 생성/삭제 시점에 원자적 UPDATE로 관리된다
     * ({@code MemberRepository.increaseProjectCount/decreaseProjectCount}) — {@code @Version} 충돌을 피한다.
     */
    @Column(name = "project_count", nullable = false)
    private Integer projectCount = 0;

    @Version
    private Long version;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 500)
    private List<MemberJobPosition> jobPositions = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 500)
    private List<MemberTechStack> memberTechStacks = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    @BatchSize(size = 500)
    private List<ProjectMember> projectMembers = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private String provider;

    private String studentId;

    private String githubUrl;

    private String blogUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Member(String email, Integer age, String password, String realName,
                   String storeFileName, Gender gender, LocalDate birth,
                   Boolean isParticipating, UserRole role, String provider,
                   String studentId, String githubUrl, String blogUrl) {
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
        this.studentId = studentId;
        this.githubUrl = githubUrl;
        this.blogUrl = blogUrl;
    }

    /**
     * 세종대 포털 인증 회원가입용 정적 팩토리 메서드.
     *
     * <p>도메인 엔티티가 서비스 레이어 DTO(Command)에 의존하지 않도록 원시/도메인 값만 받는다.
     * Command 언패킹은 호출하는 서비스의 책임이다.
     */
    public static Member createSejongMember(String studentId, String name, LocalDate birthDate,
                                            Gender gender, String githubUrl, String blogUrl,
                                            String encodedPassword, String imageUrl) {
        String email = studentId + "@sju.ac.kr";
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .realName(name)
                .birth(birthDate)
                .gender(gender)
                .storeFileName(imageUrl)
                .isParticipating(true)
                .role(UserRole.USER)
                .provider("sejong")
                .studentId(studentId)
                .githubUrl(githubUrl)
                .blogUrl(blogUrl)
                .build();
    }

    public void addJobPosition(JobPosition jobPosition) {
        jobPositions.add(new MemberJobPosition(this, jobPosition));
    }

    public void addMemberTechStack(TechStack techStack, Integer displayOrder) {
        memberTechStacks.add(new MemberTechStack(this, techStack, displayOrder));
    }

    /**
     * 회원가입 시 직무 목록을 초기화합니다.
     */
    public void initializeJobPositions(List<JobPosition> jobPositions) {
        jobPositions.forEach(this::addJobPosition);
    }

    public void updateProfile(String realName, Integer age, Gender gender,
                              Boolean isParticipating, String introduction,
                              String githubUrl, String blogUrl) {

        this.realName = realName;
        this.age = age;
        this.gender = gender;
        this.isParticipating = isParticipating;
        this.introduction = introduction;
        this.githubUrl = githubUrl;
        this.blogUrl = blogUrl;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public void setStoreFileName(String storeFileName) {
        this.storeFileName = storeFileName;
    }

    /**
     * 회원 탈퇴 처리 (소프트 삭제)
     */
    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
        this.isParticipating = false;
    }

    /**
     * 회원이 탈퇴했는지 확인
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 나이를 반환합니다.
     * age 필드가 설정되어 있으면 해당 값을 반환하고,
     * 그렇지 않으면 birth 필드에서 계산합니다.
     */
    public Integer getAge() {
        if (this.age != null) {
            return this.age;
        }
        if (this.birth == null) {
            return null;
        }
        return Period.between(this.birth, LocalDate.now()).getYears();
    }
}
