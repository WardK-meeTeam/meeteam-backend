package com.wardk.meeteam_backend.domain.project.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wardk.meeteam_backend.domain.job.JobField;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.web.mainpage.dto.request.CategoryCondition;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static java.util.Arrays.stream;
import static com.wardk.meeteam_backend.domain.job.JobPosition.values;
import static com.wardk.meeteam_backend.domain.applicant.entity.QRecruitmentState.*;
import static com.wardk.meeteam_backend.domain.member.entity.QMember.*;
import static com.wardk.meeteam_backend.domain.project.entity.QProject.*;
import static com.wardk.meeteam_backend.domain.project.entity.QProjectSkill.*;
import static com.wardk.meeteam_backend.domain.skill.entity.QSkill.skill;

public class ProjectRepositoryImpl extends Querydsl4RepositorySupport implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ProjectRepositoryImpl(JPAQueryFactory queryFactory) {
        super(Project.class);
        this.queryFactory = queryFactory;
    }

    public Page<Project> findAllSlicedForSearchAtCondition(ProjectSearchCondition condition, Pageable pageable) {

        Page<Project> projects = applyPagination(pageable, qf ->
                qf.select(project)
                        .from(project)
                        .join(project.creator, member).fetchJoin()
                        .where(
                                notDeleted(),
                                platformCategoryEq(condition.getPlatformCategory()),
                                recruitmentEq(condition.getRecruitment()),
                                projectTechNameExists(condition.getTechStack()),
                                jobFieldExists(condition.getJobField()),
                                projectCategoryEq(condition.getProjectCategory())
                        )
        );

        return projects;
    }

    @Override
    public Page<Project> findProjectsFromMainPageCondition(CategoryCondition condition, Pageable pageable) {
        return applyPagination(pageable,qf ->
                qf.select(project)
                        .from(project)
                        .join(project.creator, member).fetchJoin()
                        .where(
                                notDeleted(),
                                projectCategoryEq(condition.getProjectCategory())
                        ));
    }


    private BooleanExpression projectTechNameExists(TechStack techStack) {
        if (techStack == null) return null;
        return JPAExpressions
                .selectOne()
                .from(projectSkill)
                .leftJoin(projectSkill.skill,skill)
                .where(
                        projectSkill.project.eq(project),
                        skill.skillName.eq(techStack.getTechName())
                )
                .exists();
    }


    private BooleanExpression jobFieldExists(JobField jobField) {
        if (jobField == null) return null;

        List<JobPosition> validPositions = stream(values())
                .filter(p -> p.getJobField() == jobField)
                .toList();

        if (validPositions.isEmpty()) {
            return com.querydsl.core.types.dsl.Expressions.asBoolean(false).isTrue();
        }

        return JPAExpressions
                .selectOne()
                .from(recruitmentState)
                .where(
                        recruitmentState.project.eq(project),
                        recruitmentState.jobPosition.in(validPositions)
                )
                .exists();
    }



    private BooleanExpression platformCategoryEq(PlatformCategory platformCategory) {
        return (platformCategory == null) ? null : project.platformCategory.eq(platformCategory);
    }

    private BooleanExpression recruitmentEq(Recruitment recruitment) {
        return (recruitment == null) ? null : project.recruitmentStatus.eq(recruitment);
    }


    private BooleanExpression projectCategoryEq(ProjectCategory projectCategory) {
        return (projectCategory == null) ? null : project.projectCategory.eq(projectCategory);
    }


    private BooleanExpression notDeleted() {
        return project.isDeleted.eq(false);
    }


}