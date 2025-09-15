package com.wardk.meeteam_backend.domain.project.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.web.project.dto.ProjectSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import static com.wardk.meeteam_backend.domain.applicant.entity.QProjectCategoryApplication.*;
import static com.wardk.meeteam_backend.domain.category.entity.QBigCategory.*;
import static com.wardk.meeteam_backend.domain.category.entity.QSubCategory.*;
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

    public Slice<Project> findAllSlicedForSearchAtCondition(ProjectSearchCondition condition, Pageable pageable) {

        Slice<Project> projects = applySlicing(pageable, qf ->
                qf.select(project)
                        .from(project)
                        .join(project.creator, member).fetchJoin()
                        .where(
                                notDeleted(),
                                platformCategoryEq(condition.getPlatformCategory()),
                                recruitmentEq(condition.getRecruitment()),
                                projectTechNameExists(condition.getTechStack()),
                                bigCategoryExists(condition.getBigCategory()),
                                projectCategoryEq(condition.getProjectCategory())
                        )
        );

        return projects;
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


    private BooleanExpression bigCategoryExists(String bigCategoryName) {
        if (bigCategoryName == null || bigCategoryName.isBlank()) return null;
        return JPAExpressions
                .selectOne()
                .from(projectCategoryApplication)
                .join(projectCategoryApplication.subCategory, subCategory)
                .join(subCategory.bigCategory, bigCategory)
                .where(
                        projectCategoryApplication.project.eq(project),
                        bigCategory.name.eq(bigCategoryName)
                )
                .exists();
    }

    private BooleanExpression bigCategoryExists(String bigCategoryName) {
        if (bigCategoryName == null || bigCategoryName.isBlank()) return null;
        return JPAExpressions
                .selectOne()
                .from(projectCategoryApplication)
                .join(projectCategoryApplication.subCategory, subCategory)
                .join(subCategory.bigCategory, bigCategory)
                .where(
                        projectCategoryApplication.project.eq(project),
                        bigCategory.name.eq(bigCategoryName)
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
