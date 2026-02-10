# 테스트 컨벤션 가이드

본 문서는 특정 프로젝트 구현체 이름에 의존하지 않고, BDD 기반 테스트 코드를 팀 표준으로 작성하기 위한 공통 규칙을 정리한 문서입니다.

## 1. 목표

- 비즈니스 시나리오를 `feature` 파일에서 읽히게 작성한다.
- Step 클래스는 "행동 조합"만 담당하고, HTTP 호출/데이터 생성/DB 조회는 전용 계층에 위임한다.
- 시나리오 간 상태 오염을 제거해 독립 실행 가능하게 유지한다.

## 2. 테스트 패키지 구조와 책임

### `api`

- 역할: 외부 API 호출 래퍼(RestAssured 등)
- 권장 구성:
  - 인증 API
  - 도메인별 API(예약/주문/대여/회원 등)
  - 요청 파라미터/바디 구성 헬퍼
- 규칙:
  - Step에서 직접 HTTP 클라이언트 호출 금지
  - API 메서드명은 팀 표준에 맞춰 한글 동사형으로 작성 권장
  - 예: `대여_생성`, `예약_상태_변경`, `고객_상세_조회`
  - API 클래스 메서드는 "요청 구성 + 호출 1회"까지만 담당

### `factory`

- 역할: Given 단계용 도메인 데이터 생성
- 권장 구성:
  - 도메인별 테스트 데이터 팩토리
  - 기본값을 포함한 생성 편의 메서드
- 규칙:
  - 테스트 사전 상태 생성만 담당
  - 복잡한 생성/연관관계 구성은 Step이 아니라 Factory에 모은다

### `steps`

- 역할: Gherkin과 실행 코드 연결
- 권장 구성:
  - 인증 Step
  - 도메인별 Step
  - 테스트 실행 설정/Hook
- 규칙:
  - Given/When/Then 의도를 메서드 단위로 분리
  - Step 클래스는 시나리오 상태 공유를 위해 `TestContext`에 의존한다
  - 공통 검증 로직은 private helper로 추출
  - Step 간 상태 공유는 반드시 시나리오 스코프 컨텍스트(`TestContext`)만 사용

### `context`

- 역할: Step 클래스 간 상태 공유 저장소
- 권장 위치: `steps` 패키지 내부(예: `steps.context.TestContext`)
- 권장 구성:
  - 공통 필드(예: `accessToken`, `response`)
  - 도메인별 이너 클래스(예: `ReservationData`, `ProductData`, `CustomerData`)
- 규칙:
  - `TestContext`는 Step 계층 전용 상태이므로 `api`보다 `steps` 영역에 두는 것을 권장한다
  - 도메인 상태를 컨텍스트 최상위에 평면 필드로 늘리지 않는다
  - `TestContext` 내부 이너 클래스로 도메인 단위 묶음을 유지한다
  - Step에서는 필요한 도메인 블록만 접근한다(예: `testContext.getReservation().getId()`)

### `support`

- 역할: 테스트 공통 인프라 접근 제공
- 권장 구성:
  - API 파사드(`TestApiSupport`): 여러 API 빈을 한 곳에서 관리
  - Repository 파사드(`TestRepositorySupport`): 여러 Repository 빈을 한 곳에서 관리
  - DB 초기화/격리 유틸(`DatabaseCleaner`)
- 규칙:
  - Step에서 개별 의존성을 과도하게 주입하지 말고 Support를 통해 접근
  - 시나리오 시작 시 DB 초기화는 Hook에서 일괄 실행
  - `DatabaseCleaner`는 각 시나리오 시작 전에 테이블 데이터를 비워 테스트 간 오염을 차단한다

### `features`

- 역할: 비즈니스 시나리오 명세
- 규칙:
  - `Background`에는 공통 전제만 둔다
  - 정상/예외/엣지 케이스를 구분해 작성한다
  - 기술 용어보다 업무 용어 중심으로 작성한다

### `runner`

- 역할: Cucumber 테스트 실행 진입점
- 예시 위치: `src/test/java/.../CucumberTestRunner.java`
- 주요 어노테이션:
  - `@Suite`: JUnit 플랫폼에서 테스트 스위트로 실행
  - `@SelectClasspathResource("features")`: 실행할 feature 파일 경로 지정
  - `@ConfigurationParameter(GLUE_PROPERTY_NAME, ...)`: Step/Hook 클래스 패키지 지정
  - `@ConfigurationParameter(PLUGIN_PROPERTY_NAME, ...)`: 출력 포맷(예: `pretty`, `summary`) 지정

### `configuration`

- 역할: Cucumber와 Spring 테스트 컨텍스트 연결, 시나리오 공통 초기화 수행
- 예시 위치: `src/test/java/.../steps/CucumberSpringConfiguration.java`
- 주요 어노테이션:
  - `@CucumberContextConfiguration`: Cucumber-Spring 연동 활성화
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)`: 내장 서버를 랜덤 포트로 띄워 통합 테스트 실행
  - `@ActiveProfiles("test")`: 테스트 프로파일 적용
- 주요 책임:
  - `@Before` Hook에서 API 포트 설정
  - `DatabaseCleaner.clear()` 호출로 시나리오 간 DB 격리

### Hook vs Background (간단 정리)

- `Hook`(`@Before`, `@After`):
  - 자바 코드 레벨의 공통 전/후처리
  - 인프라 초기화(DB 정리, 포트 설정, 외부 리소스 정리)에 적합
- `Background`(feature 파일):
  - 시나리오가 공유하는 업무 전제 조건 표현
  - 로그인, 공통 도메인 데이터 준비처럼 \"읽히는 시나리오 맥락\"에 적합
- 기준:
  - 비즈니스 문장으로 보여줘야 하면 `Background`
  - 기술적/인프라 초기화면 `Hook`

## 3. 시나리오 실행 흐름(표준)

1. `feature`에서 비즈니스 문장 작성
2. `Given`에서 Factory/Repository로 사전 상태 준비
3. `When`에서 API 호출 후 `testContext.setResponse(...)`
4. `Then`에서
   - 1차: HTTP 상태/응답 검증
   - 2차: 필요 시 DB 상태 검증
5. 다음 시나리오 시작 전 DB/컨텍스트 초기화로 격리

## 4. 클래스별 역할 위임 기준

| 위치 | 해야 할 일 | 하지 말아야 할 일 |
|---|---|---|
| `feature` | 업무 시나리오/의도 표현 | 구현 디테일(엔티티/JSON 구조) 노출 |
| `steps` | 시나리오 단계 조합, 컨텍스트 저장/조회, 단언 | 직접 HTTP 호출, 복잡한 객체 생성 |
| `api` | 엔드포인트 호출, 요청 바디 구성 | 비즈니스 단언/DB 조회 |
| `factory` | 테스트 데이터 생성, 기본값 세팅 | HTTP 호출 |
| `support` | API/Repository/DB정리 진입점 제공 | 도메인 검증 로직 |
| `TestContext` | Step 간 상태 공유(토큰/응답/ID) | 비즈니스 계산/검증 |

## 5. 네이밍/작성 규칙

- Step 메서드명: 문장형(한글/영문 팀 표준 중 하나로 통일)
- API 메서드명: 한글 동사형(`대여_생성`, `상태_변경`, `상세_조회`)
- 상수: 반복 문자열(상태값/메시지)은 상수화
- 검증: 상태코드 + 메시지/DB 상태를 함께 검증
- 예외 검증: 중복 로직은 helper 메서드로 통일

## 6. 표준 샘플 코드

아래 샘플은 특정 프로젝트 클래스명 없이, 어떤 도메인에도 적용 가능한 템플릿입니다.

### 6.1 Feature 샘플

```gherkin
Feature: 자원 상태 변경
  운영자는 자원 상태를 변경할 수 있다.

  Background:
    Given 운영자로 로그인되어 있다

  Scenario: 정상 상태 변경
    Given 상태 변경 가능한 자원이 존재한다
    When 운영자가 해당 자원의 상태를 "TARGET"으로 변경한다
    Then 상태 변경이 정상 처리된다
    And 자원의 상태가 "TARGET"으로 저장된다
```

### 6.2 Steps 의존성 주입 샘플

```java
public class ReservationSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private TestContext testContext;

    @Autowired
    private TestApiSupport api;

    @Autowired
    private TestRepositorySupport repository;

    @Autowired
    private ReservationFactory reservationFactory;
}
```

### 6.3 Steps 샘플

```java
public class ResourceSteps {

    @Autowired
    private TestContext testContext;

    @Autowired
    private TestApiSupport api;

    @Autowired
    private TestRepositorySupport repository;

    @Autowired
    private ResourceFactory resourceFactory;

    @Given("상태 변경 가능한 자원이 존재한다")
    public void 상태변경_가능_자원_준비() {
        var saved = resourceFactory.createChangeableResource();
        testContext.getResource().setId(saved.getId());
    }

    @When("운영자가 해당 자원의 상태를 {string}으로 변경한다")
    public void 상태_변경_요청(String targetStatus) {
        var response = api.resource().상태_변경(
                testContext.getAccessToken(),
                testContext.getResource().getId(),
                targetStatus
        );
        testContext.setResponse(response);
    }

    @Then("상태 변경이 정상 처리된다")
    public void 성공_응답_검증() {
        assertThat(testContext.getResponse().statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Then("자원의 상태가 {string}으로 저장된다")
    public void DB_상태_검증(String expectedStatus) {
        var resource = repository.resource().findById(testContext.getResource().getId())
                .orElseThrow(() -> new AssertionError("리소스를 찾을 수 없습니다"));

        assertThat(resource.getStatus().name()).isEqualTo(expectedStatus);
    }
}
```

### 6.4 API 샘플

```java
@Component
public class ResourceAPI {

    public ExtractableResponse<Response> 상태_변경(String token, Long resourceId, String targetStatus) {
        Map<String, String> requestBody = Map.of("status", targetStatus);

        return RestAssured
                .given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                .when()
                    .patch("/admin/resources/{id}/status", resourceId)
                .then()
                    .extract();
    }
}
```

### 6.5 API 파사드 샘플 (`TestApiSupport`)

```java
@Component
public class TestApiSupport {

    @Autowired
    private AuthAPI authAPI;

    @Autowired
    private ResourceAPI resourceAPI;

    public AuthAPI auth() {
        return authAPI;
    }

    public ResourceAPI resource() {
        return resourceAPI;
    }
}
```

### 6.6 Factory 샘플

```java
@Component
public class ResourceFactory {

    @Autowired
    private ResourceRepository resourceRepository;

    public Resource createChangeableResource() {
        Resource resource = Resource.builder()
                .name("sample-resource")
                .status(ResourceStatus.INITIAL)
                .build();

        return resourceRepository.save(resource);
    }
}
```

### 6.7 Repository 파사드 샘플 (`TestRepositorySupport`)

```java
@Component
public class TestRepositorySupport {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    public ResourceRepository resource() {
        return resourceRepository;
    }

    public ReservationRepository reservation() {
        return reservationRepository;
    }
}
```

### 6.8 TestContext 샘플

```java
@Getter
@Component
@ScenarioScope
public class TestContext {

    @Setter
    private String accessToken;

    @Setter
    private ExtractableResponse<Response> response;

    private final ResourceData resource = new ResourceData();
    private final ReservationData reservation = new ReservationData();
    private final CustomerData customer = new CustomerData();

    @Getter
    @Setter
    public static class ResourceData {
        private Long id;
    }

    @Getter
    @Setter
    public static class ReservationData {
        private Long id;
        private String status;
    }

    @Getter
    @Setter
    public static class CustomerData {
        private Long id;
        private String name;
        private String email;
    }
}
```

### 6.9 DatabaseCleaner + Hook 샘플

```java
@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void clear() {
        entityManager.flush();
        // FK 제약 일시 해제 -> 전체 테이블 비우기 -> 제약 복구
    }
}
```

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @LocalServerPort
    private int port;

    @Before
    public void setUp() {
        RestAssured.port = port;
        databaseCleaner.clear();
    }
}
```

## 7. 신규 테스트 작성 체크리스트

- `feature`에 사용자 관점 시나리오를 먼저 작성했는가?
- Step에서 직접 HTTP 호출/복잡 생성을 하지 않고 `api`/`factory`에 위임했는가?
- API 메서드명을 한글 동사형으로 통일했는가?
- `TestApiSupport`/`TestRepositorySupport`를 통해 의존성 진입점을 단순화했는가?
- Step 클래스가 `TestContext`를 통해 상태를 공유하도록 작성됐는가?
- `TestContext` 내부를 도메인별 이너 클래스로 분리해 관리했는가?
- 응답 검증 + DB 검증(필요 시) 둘 다 포함했는가?
- `TestContext`에 공유 상태를 저장해 Step 간 결합을 낮췄는가?
- 시나리오가 단독 실행되어도 통과하는가?(초기화 전제)
