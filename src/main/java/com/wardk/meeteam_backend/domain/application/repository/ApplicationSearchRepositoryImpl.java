package com.wardk.meeteam_backend.domain.application.repository;

import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.web.application.dto.request.ApplicationSearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 지원자 답변(EAV) 동적 필터 검색 구현체.
 *
 * <p>EAV 특성상 필터 조건마다 답변이 <b>서로 다른 행</b>에 있으므로,
 * 조건 수만큼 {@code EXISTS (application_answer ...)} 서브쿼리를 동적으로 생성합니다.
 * (이 "조건=행마다 분리" 구조적 비용은 타입별 컬럼으로도 사라지지 않습니다.)
 *
 * <p>단, 값은 <b>타입별 컬럼</b>에 저장되므로 NUMBER 비교는 {@code valInt}(INT)를 직접 비교합니다.
 * CAST 가 필요 없어 올바르게 비교/정렬되고 인덱스도 탈 수 있습니다.
 */
@Repository
public class ApplicationSearchRepositoryImpl implements ApplicationSearchRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ProjectApplication> search(Long projectId,
                                           List<ApplicationSearchRequest.FilterCondition> filters) {
        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT a FROM ProjectApplication a
                JOIN FETCH a.applicant
                WHERE a.project.id = :projectId
                  AND a.status = 'PENDING'
                  AND a.project.isDeleted = false
                """);

        // 필터 조건마다 EXISTS 서브쿼리를 동적으로 덧붙인다. (EAV 라서 조건=행마다 분리됨)
        for (int i = 0; i < filters.size(); i++) {
            ApplicationSearchRequest.FilterCondition filter = filters.get(i);
            jpql.append(" AND EXISTS (SELECT 1 FROM ApplicationAnswer ans").append(i)
                    .append(" WHERE ans").append(i).append(".application = a")
                    .append(" AND ans").append(i).append(".question.id = :qid").append(i);

            switch (filter.operator()) {
                // NUMBER: 숫자 컬럼(valInt)을 직접 비교 - CAST 불필요, 인덱스 가능
                case GTE -> jpql.append(" AND ans").append(i).append(".valInt >= :val").append(i);
                case LTE -> jpql.append(" AND ans").append(i).append(".valInt <= :val").append(i);
                // SELECT: 문자 컬럼(valText) 정확히 일치
                case EQ -> jpql.append(" AND ans").append(i).append(".valText = :val").append(i);
                // TEXT: 문자 컬럼(valText) 특정 단어 포함
                case CONTAINS -> jpql.append(" AND ans").append(i).append(".valText LIKE :val").append(i);
            }
            jpql.append(")");
        }

        TypedQuery<ProjectApplication> query = em.createQuery(jpql.toString(), ProjectApplication.class);
        query.setParameter("projectId", projectId);

        for (int i = 0; i < filters.size(); i++) {
            ApplicationSearchRequest.FilterCondition filter = filters.get(i);
            query.setParameter("qid" + i, filter.questionId());

            switch (filter.operator()) {
                case GTE, LTE -> query.setParameter("val" + i, Integer.parseInt(filter.value()));
                case CONTAINS -> query.setParameter("val" + i, "%" + filter.value() + "%");
                case EQ -> query.setParameter("val" + i, filter.value());
            }
        }

        return query.getResultList();
    }
}