# MeeTeam Backend 테스트 컨벤션

---

## 1. 테스트 전략

| 테스트 유형 | 범위 | 목적 |
|-----------|------|------|
| 단위 테스트 | Entity | 엔티티 행동, 검증 로직 |
| 통합 테스트 | Service → Repository | 실제 DB 연동, DTO 검증 |
| 인수 테스트 | Controller → DB (E2E) | 사용자 시나리오 |

---

## 2. 패키지 구조

```
src/test/java/com/wardk/meeteam_backend/
├── unit/                          # 단위 테스트
│   └── {도메인}/
│       └── entity/                # 엔티티 행동 테스트
│
├── integration/                   # 통합 테스트
│   └── {도메인}/
│       └── service/               # Service + Repository
│
├── acceptance/cucumber/           # 인수 테스트 (기존 구조)
│
└── fixture/                       # 공통 테스트 픽스처
```

---

## 3. 단위 테스트 (Entity)

엔티티의 행동(비즈니스 로직, 검증 메서드)을 테스트합니다.

### 3.1 기본 구조

```java
@DisplayName("Project 엔티티 테스트")
class ProjectTest {

    @Nested
    @DisplayName("isLeader 메서드는")
    class IsLeader {

        @Test
        @DisplayName("생성자의 ID와 일치하면 true를 반환한다")
        void returns_true_when_creator_id_matches() {
            // given
            Member creator = MemberFixture.withId(1L);
            Project project = ProjectFixture.withCreator(creator);

            // when
            boolean result = project.isLeader(1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("생성자의 ID와 일치하지 않으면 false를 반환한다")
        void returns_false_when_creator_id_not_matches() {
            // given
            Member creator = MemberFixture.withId(1L);
            Project project = ProjectFixture.withCreator(creator);

            // when
            boolean result = project.isLeader(999L);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("validateLeaderPermission 메서드는")
    class ValidateLeaderPermission {

        @Test
        @DisplayName("리더가 아니면 CustomException을 던진다")
        void throws_exception_when_not_leader() {
            // given
            Member creator = MemberFixture.withId(1L);
            Project project = ProjectFixture.withCreator(creator);

            // when & then
            assertThatThrownBy(() -> project.validateLeaderPermission(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROJECT_MEMBER_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("create 팩토리 메서드는")
    class Create {

        @Test
        @DisplayName("모집 상태가 RECRUITING으로 초기화된다")
        void initializes_recruitment_status_as_recruiting() {
            // when
            Project project = Project.create(name, creator, category, platform);

            // then
            assertThat(project.getRecruitment()).isEqualTo(Recruitment.RECRUITING);
        }
    }
}
```

### 3.2 테스트 대상

| 메서드 패턴 | 설명 | 예시 |
|-----------|------|------|
| `is{State}()` | 상태 확인 | `isLeader()`, `isCompleted()`, `isEditable()` |
| `has{Something}()` | 소유 확인 | `hasMember()`, `hasPermission()` |
| `can{Action}()` | 행동 가능 여부 | `canEdit()`, `canDelete()` |
| `validate{Condition}()` | 검증 + 예외 | `validateLeaderPermission()`, `validateNotCompleted()` |
| `create()` | 팩토리 메서드 | 초기 상태 검증 |
| `update{Field}()` | 상태 변경 | 변경 후 상태 검증 |

---

## 4. 통합 테스트 (Service)

Service 메서드를 실제 DB와 연동하여 테스트하고, **반환되는 DTO를 검증**합니다.

### 4.1 기본 구조

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ProjectQueryService 통합 테스트")
class ProjectQueryServiceIntegrationTest {

    @Autowired
    private ProjectQueryService projectQueryService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member leader;

    @BeforeEach
    void setUp() {
        leader = memberRepository.save(MemberFixture.defaultMember());
    }

    @Nested
    @DisplayName("searchV1 메서드는")
    class SearchV1 {

        @Test
        @DisplayName("키워드가 프로젝트명에 포함된 프로젝트의 DTO를 반환한다")
        void returns_dto_containing_keyword_in_name() {
            // given
            Project saved = projectRepository.save(
                ProjectFixture.withName("AI 개발 프로젝트", leader)
            );
            projectRepository.save(ProjectFixture.withName("헬스케어 앱", leader));

            ProjectSearchRequest request = new ProjectSearchRequest(
                "개발", null, null, null, null, null, null
            );

            // when
            Page<ProjectCardResponse> result = projectQueryService.searchV1(
                request, PageRequest.of(0, 20), null
            );

            // then - DTO 필드 검증
            assertThat(result.getContent()).hasSize(1);

            ProjectCardResponse dto = result.getContent().get(0);
            assertThat(dto.projectId()).isEqualTo(saved.getId());
            assertThat(dto.projectName()).isEqualTo("AI 개발 프로젝트");
            assertThat(dto.creatorName()).isEqualTo(leader.getRealName());
            assertThat(dto.isLiked()).isFalse();  // 비로그인
        }

        @Test
        @DisplayName("DEADLINE 정렬 시 마감일 오름차순으로 DTO 목록을 반환한다")
        void returns_dtos_sorted_by_deadline_ascending() {
            // given
            Project farProject = projectRepository.save(
                ProjectFixture.withEndDate(LocalDate.now().plusMonths(2), leader, "먼 프로젝트")
            );
            Project nearProject = projectRepository.save(
                ProjectFixture.withEndDate(LocalDate.now().plusDays(3), leader, "임박 프로젝트")
            );

            ProjectSearchRequest request = new ProjectSearchRequest(
                null, null, null, null, null, null, ProjectSortType.DEADLINE
            );

            // when
            Page<ProjectCardResponse> result = projectQueryService.searchV1(
                request, PageRequest.of(0, 20), null
            );

            // then - 정렬 순서 검증
            assertThat(result.getContent())
                .extracting(ProjectCardResponse::projectName)
                .containsExactly("임박 프로젝트", "먼 프로젝트");
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 Page를 반환한다")
        void returns_empty_page_when_no_results() {
            // given
            ProjectSearchRequest request = new ProjectSearchRequest(
                "존재하지않는키워드", null, null, null, null, null, null
            );

            // when
            Page<ProjectCardResponse> result = projectQueryService.searchV1(
                request, PageRequest.of(0, 20), null
            );

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("페이징이 정상 동작한다")
        void pagination_works_correctly() {
            // given - 25개 프로젝트 생성
            for (int i = 1; i <= 25; i++) {
                projectRepository.save(ProjectFixture.withName("프로젝트 " + i, leader));
            }

            ProjectSearchRequest request = new ProjectSearchRequest(
                null, null, null, null, null, null, null
            );

            // when
            Page<ProjectCardResponse> result = projectQueryService.searchV1(
                request, PageRequest.of(0, 20), null
            );

            // then
            assertThat(result.getContent()).hasSize(20);
            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.isLast()).isFalse();
        }
    }
}
```

### 4.2 DTO 검증 포인트

```java
// 필드 값 검증
assertThat(dto.projectId()).isEqualTo(expected.getId());
assertThat(dto.projectName()).isEqualTo("AI 개발");

// 연관 데이터 변환 검증
assertThat(dto.creatorName()).isEqualTo(leader.getRealName());
assertThat(dto.categoryName()).isEqualTo(category.getDisplayName());

// 계산된 필드 검증
assertThat(dto.isLiked()).isFalse();
assertThat(dto.recruitments()).hasSize(2);

// 목록 순서 검증
assertThat(result.getContent())
    .extracting(ProjectCardResponse::projectName)
    .containsExactly("첫번째", "두번째", "세번째");
```

---

## 5. 인수 테스트 (Cucumber)

### 5.1 Feature 파일

```gherkin
# language: ko

@acceptance @project @search-v1
기능: 프로젝트 검색 V1 API

  @search @happy-path
  시나리오: 키워드로 프로젝트를 검색할 수 있다
    먼저 다음 프로젝트들이 등록되어 있다:
      | 프로젝트명           | 카테고리  |
      | AI 뉴스 요약 서비스  | AI_TECH  |
    만약 V1 API로 "뉴스" 키워드로 검색하면
    그러면 응답 코드가 "OK"이다
    그리고 검색 결과에 "AI 뉴스 요약 서비스"가 포함된다
```

### 5.2 Step 정의

```java
public class ProjectSearchV1Steps {

    @Autowired
    private TestContext context;

    @Autowired
    private TestApiSupport api;

    @만약("V1 API로 {string} 키워드로 검색하면")
    public void v1_api로_키워드로_검색하면(String keyword) {
        var response = api.projectSearch().searchV1(keyword, null, null);
        context.setResponse(response);
    }

    @그러면("응답 코드가 {string}이다")
    public void 응답_코드가_이다(String expectedCode) {
        String actualCode = context.getResponse().jsonPath().getString("code");
        assertThat(actualCode).isEqualTo(expectedCode);
    }
}
```

---

## 6. Fixture 클래스

```java
public class ProjectFixture {

    public static Project defaultProject(Member creator) {
        return Project.builder()
                .projectName("테스트 프로젝트")
                .creator(creator)
                .projectCategory(ProjectCategory.AI_TECH)
                .platformCategory(PlatformCategory.WEB)
                .recruitment(Recruitment.RECRUITING)
                .endDate(LocalDate.now().plusMonths(1))
                .build();
    }

    public static Project withName(String name, Member creator) {
        return Project.builder()
                .projectName(name)
                .creator(creator)
                .projectCategory(ProjectCategory.AI_TECH)
                .platformCategory(PlatformCategory.WEB)
                .recruitment(Recruitment.RECRUITING)
                .endDate(LocalDate.now().plusMonths(1))
                .build();
    }

    public static Project withCreator(Member creator) {
        return defaultProject(creator);
    }

    public static Project withEndDate(LocalDate endDate, Member creator, String name) {
        return Project.builder()
                .projectName(name)
                .creator(creator)
                .projectCategory(ProjectCategory.AI_TECH)
                .platformCategory(PlatformCategory.WEB)
                .recruitment(Recruitment.RECRUITING)
                .endDate(endDate)
                .build();
    }
}

public class MemberFixture {

    public static Member defaultMember() {
        return Member.builder()
                .email("test@example.com")
                .realName("테스트유저")
                .build();
    }

    public static Member withId(Long id) {
        Member member = defaultMember();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
```

---

## 7. 네이밍 규칙

### 클래스 네이밍

| 테스트 유형 | 패턴 | 예시 |
|-----------|------|------|
| 단위 (Entity) | `{Entity}Test` | `ProjectTest` |
| 통합 (Service) | `{Service}IntegrationTest` | `ProjectQueryServiceIntegrationTest` |

### 메서드 네이밍

```java
// snake_case + 행위 설명
void returns_true_when_creator_id_matches()
void throws_exception_when_not_leader()
void returns_dto_containing_keyword_in_name()
```

### DisplayName

```java
@DisplayName("생성자의 ID와 일치하면 true를 반환한다")
@DisplayName("키워드가 프로젝트명에 포함된 프로젝트의 DTO를 반환한다")
```

---

## 8. Checklist

**단위 테스트 (Entity)**
- [ ] 스프링 컨텍스트 없이 순수 Java
- [ ] `@Nested`로 메서드별 그룹화
- [ ] `@DisplayName` 한국어 설명
- [ ] 엔티티 행동 메서드 검증 (`is*`, `validate*`, `create`)
- [ ] 예외 케이스 검증

**통합 테스트 (Service)**
- [ ] `@SpringBootTest` + `@ActiveProfiles("test")`
- [ ] `@Transactional`로 테스트 격리
- [ ] Fixture 클래스로 테스트 데이터 생성
- [ ] **반환 DTO 필드 검증**
- [ ] 목록 정렬 순서 검증
- [ ] 페이징 검증

**인수 테스트**
- [ ] Feature 파일 한국어 작성
- [ ] 시나리오별 태그 (`@happy-path`, `@edge-case`)

---

*Last Updated: 2026-03-31*
