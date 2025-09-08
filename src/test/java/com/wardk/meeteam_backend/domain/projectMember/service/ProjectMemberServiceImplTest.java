package com.wardk.meeteam_backend.domain.projectMember.service;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 낙관적 락 충돌을 재현하는 순수 JPA 통합 테스트
 * - 서로 다른 영속성 컨텍스트(=트랜잭션) 2개를 만들어 같은 엔티티를 로딩
 * - 트랜잭션1이 선반영(commit)하여 version 증가
 * - 트랜잭션2가 구버전 상태로 갱신/flush 시 OptimisticLockException 발생
 */
@SpringBootTest
class ProjectMemberServiceImplTest {

    @Autowired
    private EntityManagerFactory emf;

    /**
     * 초기 데이터(서브카테고리, 지원카테고리 엔티티)를 별도 트랜잭션으로 저장하고 ID를 반환
     */
    private Long prepareInitialData() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubCategory sub = new SubCategory("백엔드");
            em.persist(sub);

            ProjectCategoryApplication pca = ProjectCategoryApplication.createProjectCategoryApplication(sub, 10);
            em.persist(pca);

            tx.commit();
            return pca.getId();
        } finally {
            if (tx.isActive()) tx.rollback();
            em.close();
        }
    }

    @Test
    @DisplayName("낙관적락 충돌 발생 테스트")
    void 낙관적락_충돌_테스트() {
        // given: 테스트 대상 엔티티 생성
        Long id = prepareInitialData();

        // 서로 다른 영속성 컨텍스트 2개 준비
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        EntityTransaction tx1 = em1.getTransaction();
        EntityTransaction tx2 = em2.getTransaction();

        try {
            tx1.begin();
            tx2.begin();

            // 트랜잭션1/2에서 같은 레코드 로딩 (각자 버전 스냅샷 가짐)
            ProjectCategoryApplication p1 = em1.find(ProjectCategoryApplication.class, id);
            ProjectCategoryApplication p2 = em2.find(ProjectCategoryApplication.class, id);

            // when: 트랜잭션1이 선반영 → version 증가
            p1.increaseCurrentCount();
            em1.flush();
            tx1.commit();

            // then: 트랜잭션2는 구버전 상태로 갱신 시도 → OptimisticLockException
            assertThrows(OptimisticLockException.class, () -> {
                p2.increaseCurrentCount();
                em2.flush(); // 여기서 버전 충돌 발생
            });
        } finally {
            // 정리
            if (tx2.isActive()) {
                try { tx2.rollback(); } catch (Exception ignored) {}
            }
            if (em1.isOpen()) em1.close();
            if (em2.isOpen()) em2.close();
        }
    }
}