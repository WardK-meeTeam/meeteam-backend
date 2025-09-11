package com.wardk.meeteam_backend.domain.project.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wardk.meeteam_backend.domain.project.entity.*;
import com.wardk.meeteam_backend.web.project.dto.ProjectSearchCondition;
import com.wardk.meeteam_backend.web.project.dto.ProjectSearchResponse;
import com.wardk.meeteam_backend.web.project.dto.QProjectSearchResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

import static com.wardk.meeteam_backend.domain.applicant.entity.QProjectCategoryApplication.*;
import static com.wardk.meeteam_backend.domain.category.entity.QBigCategory.*;
import static com.wardk.meeteam_backend.domain.category.entity.QSubCategory.*;
import static com.wardk.meeteam_backend.domain.member.entity.QMember.*;
import static com.wardk.meeteam_backend.domain.project.entity.QProject.*;

public class ProjectRepositoryImpl extends Querydsl4RepositorySupport implements ProjectRepositoryCustom {


    private final JPAQueryFactory queryFactory;

    public ProjectRepositoryImpl(JPAQueryFactory queryFactory) {
        super(Project.class);
        this.queryFactory = queryFactory;
    }

    @Override
    public Slice<ProjectSearchResponse> findAllSlicedForSearchAtCondition(ProjectSearchCondition condition, Pageable pageable) {

        // 1) ID 선조회
        List<Long> ids = queryFactory
                .select(project.id)
                .from(project)
                .where(
                        notDeleted(),
                        platformCategoryEq(condition.getPlatformCategory()),
                        recruitmentEq(condition.getRecruitment()),
                        bigCategoryExists(condition.getBigCategory()),
                        projectCategoryEq(condition.getProjectCategory())
                )
                .fetch();


        // 2) 본문조회 → applySlicing에 위임
        return applySlicing(pageable, queryFactory ->
                queryFactory
                        .select(new QProjectSearchResponse(
                                project.id,
                                project.platformCategory,
                                project.name,
                                member.realName,
                                project.createdAt,
                                project.projectCategory
                        ))
                        .from(project)
                        .join(project.creator, member)
                        .where(project.id.in(ids))
        );

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


    private BooleanExpression bigCategoryEq(String category) {
        if (category == null || category.isBlank()) return null;
        return bigCategory.name.eq(category);
    }

    private BooleanExpression notDeleted() {
        return project.isDeleted.eq(false);
    }


}
