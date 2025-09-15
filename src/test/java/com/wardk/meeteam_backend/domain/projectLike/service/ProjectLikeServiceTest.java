package com.wardk.meeteam_backend.domain.projectLike.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectLike.repository.ProjectLikeRepository;
import com.wardk.meeteam_backend.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.transaction.TestTransaction;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@Transactional
@DataJpaTest
@Import({QueryDslConfig.class, ProjectLikeService.class})
class ProjectLikeServiceTest {

    @Autowired
    ProjectLikeService projectLikeService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    ProjectLikeRepository projectLikeRepository;

    @Test
    void 비관적락_좋아요_집계_정확성_테스트() throws InterruptedException {
        // given
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Project project = new Project();
        project.setName("Test Project");
        project.setDescription("Test Description");
        projectRepository.save(project);

        List<Member> members = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Member m = Member.builder()
                    .email("test" + i + "@gmail.com")
                    .role(UserRole.USER)
                    .build();
            members.add(memberRepository.save(m));
        }

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        long start = System.currentTimeMillis();


        System.out.println("-------------");
        // when
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    projectLikeService.toggleWithPessimistic(project.getId(), members.get(idx).getEmail());
                } finally {
                    latch.countDown();
                }
            });
        }
        System.out.println("----------");

        long end = System.currentTimeMillis();

        System.out.println("비관적락time = " +  (end - start));

        latch.await();
        executor.shutdown();

        // then
        Project updated = projectRepository.findById(project.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(threadCount, updated.getLikeCount()),
                () -> assertEquals(threadCount,
                        projectLikeRepository.countByProjectId(project.getId()))
        );
    }

}
