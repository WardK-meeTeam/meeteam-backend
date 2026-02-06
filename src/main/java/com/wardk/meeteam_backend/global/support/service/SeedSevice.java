package com.wardk.meeteam_backend.global.support.service;

import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.SubCategoryRepository;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.service.ProjectMemberService;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Profile("local")
@Service
@RequiredArgsConstructor
@Transactional
public class SeedSevice {


    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SkillRepository skillRepository;
    private final ProjectMemberService projectMemberService;

    private final Faker faker = new Faker(new Locale("ko", "KR"));

    // DB에 이미 존재하는 소분류 이름 (표기 정확히 일치)
    private static final List<String> SUB_NAMES = List.of(
            "프로덕트 매니저/오너","그래픽디자인","UI/UX디자인","모션 디자인","BX/브랜드 디자인",
            "웹프론트엔드","iOS","안드로이드","크로스플랫폼","웹서버","AI","DBA/빅데이터/DS","기타"
    );

    /** 프로젝트 count개 생성하고 생성된 ID 리스트 반환 */
    public List<Long> seedProjects(int count) {
        Map<String, SubCategory> subDict = loadSubDictOrThrow();
        List<Member> creators = ensureSomeMembers(20);

        Optional<Skill> javaOpt = skillRepository.findBySkillName("Java");
        Optional<Skill> springOpt = skillRepository.findBySkillName("Spring");

        List<Long> createdIds = Collections.synchronizedList(new ArrayList<>(count));

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                Member creator = pick(creators);
                LocalDate end = LocalDate.now().plusDays(faker.number().numberBetween(15, 90));

                ProjectCategory projectCat = randomEnum(ProjectCategory.values());
                PlatformCategory platform = randomEnum(PlatformCategory.values());

                Project project = Project.createProject(
                        creator,
                        "hi" + faker.company().buzzword() + " 프로젝트",
                        faker.lorem().sentence(12),
                        projectCat,
                        platform,
                        null,
                        faker.bool().bool(),
                        randomPastDateTime(300).toLocalDate()
                );

                // 모집 소분류
                List<String> shuffled = new ArrayList<>(SUB_NAMES);
                Collections.shuffle(shuffled, faker.random().getRandomInternal());
                int slots = faker.number().numberBetween(2, 4);
                for (int k = 0; k < slots; k++) {
                    SubCategory sc = subDict.get(shuffled.get(k));
                    int need = faker.number().numberBetween(1, 4);
                    RecruitmentState recruitmentState = RecruitmentState.createRecruitmentState(sc, need);
                    project.addRecruitment(recruitmentState);
                }

                javaOpt.ifPresent(s -> project.addProjectSkill(ProjectSkill.createProjectSkill(s)));
                springOpt.ifPresent(s -> project.addProjectSkill(ProjectSkill.createProjectSkill(s)));



                int count2 = ThreadLocalRandom.current().nextInt(1, 3); // 3~8개 랜덤
                List<Skill> picks = skillRepository.findRandomSkills(count2);

                for (Skill s : picks) {
                    project.addProjectSkill(ProjectSkill.create(project, s));
                }

                projectRepository.save(project);

                LocalDateTime ts = randomPastDateTime(90);
                projectRepository.overrideTimestamps(project.getId(), ts);

                SubCategory creatorSub = subDict.getOrDefault("웹프론트엔드", pick(new ArrayList<>(subDict.values())));
                projectMemberService.addCreator(project.getId(), creator.getId(), creatorSub);

                createdIds.add(project.getId());
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return createdIds;
    }

    // ===== helpers =====
    private Map<String, SubCategory> loadSubDictOrThrow() {
        List<SubCategory> list = subCategoryRepository.findByNameIn(SUB_NAMES);
        Map<String, SubCategory> map = list.stream()
                .collect(Collectors.toMap(SubCategory::getName, x -> x));
        List<String> missing = SUB_NAMES.stream().filter(n -> !map.containsKey(n)).toList();
        if (!missing.isEmpty()) throw new IllegalStateException("DB에 없는 소분류: " + missing);
        return map;
    }

    private List<Member> ensureSomeMembers(int n) {
        List<Member> existed = memberRepository.findAll();
        if (!existed.isEmpty()) return existed;

        List<Member> newbies = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            newbies.add(Member.createForTest(
                    faker.internet().emailAddress(),
                    faker.name().fullName()
            ));
        }
        return memberRepository.saveAll(newbies);
    }

    private <T> T pick(List<T> list) { return list.get(faker.random().nextInt(list.size())); }
    private <E extends Enum<E>> E randomEnum(E[] values) { return values[faker.random().nextInt(values.length)]; }

    /**
     * Returns a random past LocalDateTime within the last `daysBack` days.
     * Example: daysBack=90 -> anywhere between now-90d and now.
     */
    private LocalDateTime randomPastDateTime(int daysBack) {
        long now = System.currentTimeMillis();
        long earliest = now - (long) daysBack * 24L * 60L * 60L * 1000L;
        long randomMillis = ThreadLocalRandom.current().nextLong(earliest, now);
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(randomMillis), java.time.ZoneId.systemDefault());
    }
}
