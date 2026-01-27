package com.wardk.meeteam_backend.acceptance.cucumber.factory;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 테스트용 프로젝트 생성 팩토리
 */
@Component
@RequiredArgsConstructor
public class ProjectFactory {

    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;

    /**
     * 기본 프로젝트 생성
     */
    public Project createProject(String name, Member creator) {
        return createProject(name, creator, ProjectCategory.ETC);
    }

    /**
     * 카테고리를 지정하여 프로젝트 생성
     */
    public Project createProject(String name, Member creator, ProjectCategory category) {
        return projectRepository.save(Project.builder()
                .name(name)
                .creator(creator)
                .description(name + " 프로젝트입니다.")
                .projectCategory(category)
                .platformCategory(PlatformCategory.WEB)
                .imageUrl("https://example.com/image.jpg")
                .offlineRequired(false)
                .status(ProjectStatus.PLANNING)
                .recruitmentStatus(Recruitment.RECRUITING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .isDeleted(false)
                .build());
    }

    /**
     * 상세 옵션을 지정하여 프로젝트 생성
     */
    public Project createProjectWithDetails(
            String name,
            Member creator,
            ProjectCategory category,
            PlatformCategory platform,
            Recruitment recruitmentStatus
    ) {
        return projectRepository.save(Project.builder()
                .name(name)
                .creator(creator)
                .description(name + " 프로젝트입니다.")
                .projectCategory(category)
                .platformCategory(platform)
                .imageUrl("https://example.com/image.jpg")
                .offlineRequired(false)
                .status(ProjectStatus.PLANNING)
                .recruitmentStatus(recruitmentStatus)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .isDeleted(false)
                .build());
    }

    /**
     * 기술 스택을 포함한 프로젝트 생성
     */
    public Project createProjectWithSkills(String name, Member creator, List<String> skillNames) {
        Project project = createProject(name, creator);

        skillNames.forEach(skillName -> {
            Skill skill = skillRepository.findBySkillName(skillName)
                    .orElseGet(() -> skillRepository.save(new Skill(skillName)));
            project.addProjectSkill(ProjectSkill.create(project, skill));
        });

        return projectRepository.save(project);
    }

    /**
     * 완료된 프로젝트 생성
     */
    public Project createCompletedProject(String name, Member creator) {
        return projectRepository.save(Project.builder()
                .name(name)
                .creator(creator)
                .description(name + " 프로젝트입니다.")
                .projectCategory(ProjectCategory.ETC)
                .platformCategory(PlatformCategory.WEB)
                .imageUrl("https://example.com/image.jpg")
                .offlineRequired(false)
                .status(ProjectStatus.COMPLETED)
                .recruitmentStatus(Recruitment.CLOSED)
                .startDate(LocalDate.now().minusMonths(3))
                .endDate(LocalDate.now())
                .isDeleted(false)
                .build());
    }

    /**
     * 모집 마감된 프로젝트 생성
     */
    public Project createClosedProject(String name, Member creator) {
        return projectRepository.save(Project.builder()
                .name(name)
                .creator(creator)
                .description(name + " 프로젝트입니다.")
                .projectCategory(ProjectCategory.ETC)
                .platformCategory(PlatformCategory.WEB)
                .imageUrl("https://example.com/image.jpg")
                .offlineRequired(false)
                .status(ProjectStatus.PLANNING)
                .recruitmentStatus(Recruitment.CLOSED)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .isDeleted(false)
                .build());
    }

    /**
     * 대량 프로젝트 생성 (성능 테스트용)
     */
    public void createBulkProjects(Member creator, int count) {
        for (int i = 0; i < count; i++) {
            createProject("프로젝트 " + (i + 1), creator);
        }
    }

    /**
     * 카테고리별 대량 프로젝트 생성
     */
    public void createBulkProjectsByCategory(Member creator, ProjectCategory category, int count) {
        for (int i = 0; i < count; i++) {
            createProject(category.name() + " 프로젝트 " + (i + 1), creator, category);
        }
    }
}
