package com.wardk.meeteam_backend.domain.project.entity;

import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.project.service.dto.ProjectPostCommand;
import com.wardk.meeteam_backend.domain.projectlike.entity.ProjectLike;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectApplication;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectPostRequest;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.wardk.meeteam_backend.global.response.ErrorCode.PROJECT_ALREADY_COMPLETED;

@Data
@Entity
@NoArgsConstructor
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Member creator;

    @Column(name = "project_name")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ProjectCategory projectCategory;

    @Enumerated(value = EnumType.STRING)
    private PlatformCategory platformCategory;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(value = EnumType.STRING)
    private Recruitment recruitmentStatus;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentDeadlineType recruitmentDeadlineType;

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean isDeleted;

    @Column(nullable = false)
    private Integer likeCount = 0;

    private String githubRepositoryUrl;

    private String communicationChannelUrl;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectSkill> projectSkills = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectLike> projectLikes = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentState> recruitments = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectRepo> repos = new ArrayList<>();

    public void increaseLike() {
        this.likeCount++;
    }

    public void decreaseLike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    @Builder
    public Project(
            Member creator,
            String name,
            String description,
            ProjectCategory projectCategory,
            PlatformCategory platformCategory,
            String imageUrl,
            Recruitment recruitmentStatus,
            RecruitmentDeadlineType recruitmentDeadlineType,
            LocalDate startDate,
            LocalDate endDate,
            boolean isDeleted,
            String githubRepositoryUrl,
            String communicationChannelUrl
    ) {
        this.creator = creator;
        this.name = name;
        this.description = description;
        this.projectCategory = projectCategory;
        this.platformCategory = platformCategory;
        this.imageUrl = imageUrl;
        this.recruitmentStatus = recruitmentStatus;
        this.recruitmentDeadlineType = recruitmentDeadlineType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.githubRepositoryUrl = githubRepositoryUrl;
        this.communicationChannelUrl = communicationChannelUrl;
        this.isDeleted = isDeleted;
    }

    public static Project createProject(
            ProjectPostCommand projectPostCommand,
            Member creator,
            String imageUrl
    ) {
        return Project.builder()
                .creator(creator)
                .name(projectPostCommand.projectName())
                .description(projectPostCommand.description())
                .projectCategory(projectPostCommand.projectCategory())
                .platformCategory(projectPostCommand.platformCategory())
                .imageUrl(imageUrl)
                .recruitmentStatus(Recruitment.RECRUITING)
                .recruitmentDeadlineType(projectPostCommand.recruitmentDeadlineType())
                .startDate(LocalDate.now())
                .endDate(projectPostCommand.recruitmentDeadlineType().equals(RecruitmentDeadlineType.END_DATE) ? projectPostCommand.endDate() : null)
                .githubRepositoryUrl(projectPostCommand.githubRepositoryUrl())
                .communicationChannelUrl(projectPostCommand.communicationChannelUrl())
                .isDeleted(false)
                .build();
    }

    public void joinMember(ProjectMember projectMember) {
        members.add(projectMember);
        projectMember.assignProject(this);
    }

    public void addRecruitment(RecruitmentState recruitment) {
        this.recruitments.add(recruitment);
        recruitment.assignProject(this);
    }

    public void addProjectSkill(ProjectSkill projectSkill) {
        this.projectSkills.add(projectSkill);
        projectSkill.assignProject(this);
    }

    public void addRepo(ProjectRepo repo) {
        this.repos.add(repo);
        repo.setProject(this);
    }

    public void updateProject(String name, String description, ProjectCategory projectCategory, PlatformCategory platformCategory,
                              String imageUrl, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.projectCategory = projectCategory;
        this.platformCategory = platformCategory;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateRecruitments(List<RecruitmentState> recruitments) {
        Map<Long, RecruitmentState> current = this.recruitments.stream()
                .collect(Collectors.toMap(r -> r.getJobPosition().getId(), r -> r));

        for (RecruitmentState newPca : recruitments) {
            RecruitmentState existing = current.get(newPca.getJobPosition().getId());

            if(existing != null) {
                int oldCurrentCount = existing.getCurrentCount();
                int oldRecruitmentCount = existing.getRecruitmentCount();
                int newRecruitmentCount = newPca.getRecruitmentCount();

                // 기존 모집에 참여자가 있었던 경우
                if (oldCurrentCount > 0) {
                    // 모집이 완료된 경우는 인원을 늘리는 것만 허용
                    if (oldCurrentCount == oldRecruitmentCount && newRecruitmentCount < oldRecruitmentCount) {
                        throw new CustomException(ErrorCode.RECRUITMENT_ALREADY_COMPLETED);
                    }

                    existing.updateRecruitmentCount(newRecruitmentCount);
                }

            } else {
                this.addRecruitment(newPca);
            }
        }

        this.recruitments.removeIf(existing -> recruitments.stream()
                .noneMatch(n -> n.getJobPosition().getId().equals(existing.getJobPosition().getId()))
                && existing.getCurrentCount() == 0
        );
    }

    public void updateProjectSkills(List<ProjectSkill> projectSkills) {
        this.projectSkills.clear();
        for (ProjectSkill projectSkill : projectSkills) {
            this.addProjectSkill(projectSkill);
        }
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void endProject() {
        if (recruitmentStatus.equals(Recruitment.CLOSED)) {
            throw new CustomException(PROJECT_ALREADY_COMPLETED);
        }

        this.recruitmentStatus = Recruitment.CLOSED;
        this.endDate = LocalDate.now();
    }


    public void updateRecruitmentsStatus() {
        if (this.recruitmentDeadlineType != RecruitmentDeadlineType.RECRUITMENT_COMPLETED) {
            return;
        }

        boolean isClosed = this.recruitments.stream()
                .allMatch(r -> r.getCurrentCount() >= r.getRecruitmentCount());

        if (isClosed) {
            this.recruitmentStatus = Recruitment.CLOSED;
        }
    }

    public boolean isCompleted() {
        return recruitmentStatus.equals(Recruitment.CLOSED);
    }
}
