# 인수 테스트 전략 (Acceptance Test Strategy)

이 문서는 MeeTeam 프로젝트의 인수 테스트(Acceptance Test) 전략을 정의합니다. 모든 인수 테스트는 이 문서에 정의된 규칙과 표준을 따라야 합니다.

---

## 1. 핵심 기술 스택

인수 테스트는 아래의 기술 스택을 기반으로 작성합니다.

| 기술 | 역할 | 설명 |
|:---|:---|:---|
| **JUnit 5** | 테스트 프레임워크 | `@Test`, `@DisplayName`, `@BeforeEach`, `@Nested` 등의 어노테이션을 활용하여 테스트 구조를 명확하게 정의합니다. |
| **RestAssured** | API 테스트 | HTTP 요청을 보내고 응답을 검증하는 과정을 직관적인 DSL(Domain-Specific Language)로 작성할 수 있습니다. |
| **AssertJ** | 검증 라이브러리 | 풍부한 API와 명료한 문법을 통해 가독성 높은 검증 코드를 작성할 수 있습니다. |
| **H2 Database** | 테스트 DB | 인메모리 데이터베이스로 테스트 격리 및 빠른 실행을 보장합니다. |
| **Testcontainers** | 통합 테스트 | 실제 MySQL, Redis 등을 Docker 컨테이너로 실행하여 운영 환경과 유사한 테스트가 필요할 때 사용합니다. |

### 의존성 (build.gradle)

```groovy
dependencies {
    // 테스트 프레임워크
    testImplementation 'org.springframework.boot:spring-boot-starter-test'

    // RestAssured
    testImplementation 'io.rest-assured:rest-assured:5.4.0'

    // Testcontainers (선택사항 - 동시성 테스트 시 사용)
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
    testImplementation 'org.testcontainers:mysql:1.19.3'
}
```

---

## 2. 코딩 컨벤션

일관성 있는 테스트 코드를 유지하기 위해 다음 코딩 컨벤션을 준수합니다.

### 2.1 테스트 클래스 이름

- 테스트 대상이 되는 기능(Feature)이나 도메인 이름 뒤에 `AcceptanceTest` 접미사를 붙입니다.
- **예시**:
  - `ProjectSearchAcceptanceTest` - 프로젝트 검색 인수 테스트
  - `MemberAddAcceptanceTest` - 멤버 추가 인수 테스트
  - `ApplicationDecisionAcceptanceTest` - 지원 승인/거절 인수 테스트

### 2.2 테스트 메소드 이름

- 테스트 시나리오를 **한글**로 명확하게 설명하는 방식을 사용합니다.
- `@DisplayName`에 시나리오를 자연어로 작성하고, 메소드명은 동일한 내용을 snake_case 또는 한글로 작성합니다.
- **패턴**: `[상황]_[동작]_[결과]`
- **예시**:
  - `모집_정원이_가득_찬_상태에서_지원을_승인하면_승인이_거부된다()`
  - `50명이_동시에_지원해도_정원을_초과하지_않는다()`

### 2.3 내부 주석 스타일

- 모든 테스트는 `given`, `when`, `then` 구조를 주석으로 명시합니다.
- 각 단계가 무엇을 **준비**하고, 무엇을 **실행**하며, 무엇을 **검증**하는지 명확히 표현합니다.

```java
@Test
void 테스트_메소드() {
    // given - 테스트 사전 조건 설명
    ...

    // when - 테스트 대상 동작 설명
    ...

    // then - 기대 결과 검증 설명
    ...
}
```

### 2.4 Helper 메소드 네이밍

- API 호출 헬퍼: `[동작]을_요청한다()` 형태
- 데이터 생성 헬퍼: `[데이터]_생성()` 형태
- **예시**:
  - `프로젝트_검색을_요청한다()`
  - `지원_승인을_요청한다()`
  - `프로젝트_요청_생성()`

---

## 3. 파일 위치 규칙

### 3.1 디렉토리 구조

```
src/test/java/com/wardk/meeteam_backend/
├── acceptance/                          # 인수 테스트 루트
│   ├── common/                          # 공통 유틸리티
│   │   ├── AcceptanceTest.java          # 인수 테스트 기본 클래스
│   │   ├── DatabaseCleaner.java         # DB 초기화 유틸리티
│   │   └── AuthSupport.java             # 인증 헬퍼
│   ├── project/                         # 프로젝트 관련 인수 테스트
│   │   ├── ProjectSearchAcceptanceTest.java
│   │   └── ProjectSearchSteps.java      # Step 정의 (선택사항)
│   ├── member/                          # 멤버 관련 인수 테스트
│   │   ├── MemberAddAcceptanceTest.java
│   │   └── ConcurrencyAcceptanceTest.java
│   ├── application/                     # 지원 관련 인수 테스트
│   │   └── ApplicationDecisionAcceptanceTest.java
│   ├── auth/                            # 인증 관련 인수 테스트
│   │   └── AuthAcceptanceTest.java
│   └── file/                            # 파일 업로드 인수 테스트
│       └── FileUploadAcceptanceTest.java
└── support/                             # 테스트 지원 클래스
    └── TestFixture.java                 # 테스트 데이터 팩토리
```

### 3.2 파일 위치 원칙

| 파일 유형 | 위치 | 설명 |
|:---|:---|:---|
| 인수 테스트 클래스 | `acceptance/{domain}/` | 도메인별 하위 패키지에 배치 |
| 공통 기본 클래스 | `acceptance/common/` | 모든 인수 테스트가 상속받는 기본 클래스 |
| 테스트 픽스처 | `support/` | 테스트 데이터 생성 유틸리티 |
| 테스트 설정 | `resources/application-test.yml` | 테스트 환경 설정 |

---

## 4. 테스트 설정

### 4.1 application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  # 테스트 환경에서 외부 서비스 비활성화
  cloud:
    aws:
      s3:
        enabled: false

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
```

---

## 5. 표준 샘플 코드 (Golden Sample)

아래는 위에서 정의한 모든 규칙을 준수하는 표준 샘플 코드입니다. 새로운 인수 테스트를 작성할 때 이 코드를 참고하십시오.

### 5.1 기본 클래스 (AcceptanceTest.java)

```java
package com.wardk.meeteam_backend.acceptance.common;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * 인수 테스트 기본 클래스
 * 모든 인수 테스트는 이 클래스를 상속받습니다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleaner.execute();
    }
}
```

### 5.2 DB 초기화 유틸리티 (DatabaseCleaner.java)

```java
package com.wardk.meeteam_backend.acceptance.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 테스트 간 데이터 격리를 위한 DB 초기화 유틸리티
 */
@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @Transactional
    public void execute() {
        entityManager.flush();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        for (String tableName : getTableNames()) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        }

        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }

    private List<String> getTableNames() {
        if (tableNames == null) {
            tableNames = entityManager.getMetamodel().getEntities().stream()
                    .filter(entity -> entity.getJavaType().isAnnotationPresent(Table.class))
                    .map(entity -> entity.getJavaType().getAnnotation(Table.class).name())
                    .collect(Collectors.toList());
        }
        return tableNames;
    }
}
```

### 5.3 인증 헬퍼 (AuthSupport.java)

```java
package com.wardk.meeteam_backend.acceptance.common;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * 인증 관련 헬퍼 메소드
 */
public class AuthSupport {

    private static final String API_AUTH_LOGIN = "/api/auth/login";

    /**
     * 테스트용 사용자로 로그인하여 Access Token을 반환합니다.
     */
    public static String 로그인_후_토큰_획득(String email, String password) {
        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);

        ExtractableResponse<Response> response = given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post(API_AUTH_LOGIN)
                .then().log().all()
                .extract();

        return response.jsonPath().getString("result.accessToken");
    }
}
```

### 5.4 프로젝트 검색 인수 테스트 (Golden Sample)

```java
package com.wardk.meeteam_backend.acceptance.project;

import com.wardk.meeteam_backend.acceptance.common.AcceptanceTest;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프로젝트 검색 인수 테스트
 *
 * 관련 인수 조건:
 * - 프로젝트 100개 상태에서 검색 결과가 2초 이내에 표시되어야 한다
 */
@SuppressWarnings("NonAsciiCharacters")
@DisplayName("프로젝트 검색 인수 테스트")
public class ProjectSearchAcceptanceTest extends AcceptanceTest {

    private static final String API_PROJECTS = "/api/projects";
    private static final String API_PROJECTS_SEARCH = "/api/projects/search";

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

    @BeforeEach
    void setUpTestData() {
        // 테스트용 멤버 생성
        testMember = memberRepository.save(Member.builder()
                .email("test@meeteam.com")
                .nickname("테스터")
                .build());
    }

    @Nested
    @DisplayName("프로젝트 목록 조회")
    class 프로젝트_목록_조회 {

        /**
         * 시나리오: 사용자가 메인 페이지에서 프로젝트 목록을 조회한다.
         * given: 프로젝트가 등록되어 있고,
         * when: 사용자가 프로젝트 목록을 요청하면,
         * then: 프로젝트 목록이 반환된다.
         */
        @Test
        @DisplayName("프로젝트가_존재하면_목록이_반환된다")
        void 프로젝트가_존재하면_목록이_반환된다() {
            // given - 프로젝트 2개 생성
            프로젝트_생성("AI 챗봇 개발", "웹 개발");
            프로젝트_생성("모바일 앱 개발", "앱 개발");

            // when - 프로젝트 목록 조회 요청
            ExtractableResponse<Response> response = 프로젝트_목록_조회를_요청한다();

            // then - 프로젝트 목록이 반환됨
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.jsonPath().getString("code")).isEqualTo("COMMON200");
            assertThat(response.jsonPath().getList("result")).hasSize(2);
        }

        /**
         * 시나리오: 프로젝트 검색 성능을 검증한다.
         * given: 프로젝트 100개가 등록되어 있고,
         * when: 사용자가 프로젝트 목록을 요청하면,
         * then: 2초 이내에 응답이 반환된다.
         */
        @Test
        @DisplayName("프로젝트_100개_상태에서_2초_이내에_응답한다")
        void 프로젝트_100개_상태에서_2초_이내에_응답한다() {
            // given - 프로젝트 100개 생성
            for (int i = 0; i < 100; i++) {
                프로젝트_생성("프로젝트 " + i, "카테고리 " + (i % 5));
            }

            // when - 응답 시간 측정
            long startTime = System.currentTimeMillis();
            ExtractableResponse<Response> response = 프로젝트_목록_조회를_요청한다();
            long endTime = System.currentTimeMillis();

            // then - 2초(2000ms) 이내 응답
            long responseTime = endTime - startTime;
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(responseTime).isLessThan(2000L);
        }
    }

    @Nested
    @DisplayName("프로젝트 필터 검색")
    class 프로젝트_필터_검색 {

        /**
         * 시나리오: 사용자가 카테고리 필터로 프로젝트를 검색한다.
         * given: 다양한 카테고리의 프로젝트가 등록되어 있고,
         * when: 사용자가 특정 카테고리로 검색하면,
         * then: 해당 카테고리의 프로젝트만 반환된다.
         */
        @Test
        @DisplayName("카테고리_필터로_검색하면_해당_카테고리_프로젝트만_반환된다")
        void 카테고리_필터로_검색하면_해당_카테고리_프로젝트만_반환된다() {
            // given - 다양한 카테고리의 프로젝트 생성
            프로젝트_생성("웹 프로젝트 1", "WEB");
            프로젝트_생성("웹 프로젝트 2", "WEB");
            프로젝트_생성("앱 프로젝트", "APP");

            // when - 카테고리 필터로 검색
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("category", "WEB");
            ExtractableResponse<Response> response = 프로젝트_검색을_요청한다(searchParams);

            // then - WEB 카테고리 프로젝트만 반환
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.jsonPath().getList("result")).hasSize(2);
        }
    }

    // --- Helper Methods ---

    private ExtractableResponse<Response> 프로젝트_목록_조회를_요청한다() {
        return given().log().all()
                .when()
                .get(API_PROJECTS)
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> 프로젝트_검색을_요청한다(Map<String, Object> params) {
        return given().log().all()
                .queryParams(params)
                .when()
                .get(API_PROJECTS_SEARCH)
                .then().log().all()
                .extract();
    }

    private Project 프로젝트_생성(String name, String category) {
        return projectRepository.save(Project.builder()
                .name(name)
                .description(name + " 설명입니다.")
                .creator(testMember)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build());
    }
}
```

### 5.5 동시성 테스트 샘플 (ConcurrencyAcceptanceTest.java)

```java
package com.wardk.meeteam_backend.acceptance.member;

import com.wardk.meeteam_backend.acceptance.common.AcceptanceTest;
import com.wardk.meeteam_backend.acceptance.common.AuthSupport;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 멤버 추가 동시성 인수 테스트
 *
 * 관련 인수 조건:
 * - 50명 동시 지원 시에도 모집 정원을 초과 승인하는 일이 없어야 한다
 */
@SuppressWarnings("NonAsciiCharacters")
@DisplayName("멤버 추가 동시성 인수 테스트")
public class ConcurrencyAcceptanceTest extends AcceptanceTest {

    private static final String API_APPLICATION_DECIDE = "/api/projects/{projectId}/applications/{applicationId}/decide";

    @Autowired
    private ProjectRepository projectRepository;

    private Project testProject;
    private String leaderToken;

    @BeforeEach
    void setUpTestData() {
        // 테스트 프로젝트 생성 (모집 정원: 3명)
        // testProject = 프로젝트_생성_모집정원(3);
        // leaderToken = AuthSupport.로그인_후_토큰_획득("leader@test.com", "password");
    }

    /**
     * 시나리오: 50명이 동시에 지원해도 모집 정원(3명)을 초과하지 않는다.
     * given: 모집 정원이 3명인 프로젝트에 50명이 지원한 상태이고,
     * when: 팀장이 50명의 지원을 동시에 승인 시도하면,
     * then: 정확히 3명만 승인되고 나머지는 거부된다.
     */
    @Test
    @DisplayName("50명이_동시에_지원해도_정원을_초과하지_않는다")
    void 동시성_테스트_정원_초과_방지() throws InterruptedException {
        // given - 50명의 지원자 생성
        int numberOfApplicants = 50;
        int maxRecruitment = 3;
        List<Long> applicationIds = new ArrayList<>();
        // for (int i = 0; i < numberOfApplicants; i++) {
        //     applicationIds.add(지원서_생성(testProject.getId()));
        // }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfApplicants);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        // when - 50명의 지원을 동시에 승인 시도
        for (Long applicationId : applicationIds) {
            executorService.submit(() -> {
                try {
                    ExtractableResponse<Response> response = 지원_승인을_요청한다(
                            testProject.getId(),
                            applicationId,
                            leaderToken
                    );
                    statusCodes.add(response.statusCode());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확히 3명만 승인됨
        long successCount = statusCodes.stream()
                .filter(code -> code == HttpStatus.OK.value())
                .count();

        assertThat(successCount).isEqualTo(maxRecruitment);

        // DB에서 실제 멤버 수 확인
        // int actualMemberCount = 프로젝트_멤버_수_조회(testProject.getId());
        // assertThat(actualMemberCount).isEqualTo(maxRecruitment);
    }

    // --- Helper Methods ---

    private ExtractableResponse<Response> 지원_승인을_요청한다(Long projectId, Long applicationId, String token) {
        return given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"decision\": \"APPROVED\"}")
                .when()
                .post(API_APPLICATION_DECIDE, projectId, applicationId)
                .then().log().all()
                .extract();
    }
}
```

---

## 6. 체크리스트

새로운 인수 테스트를 작성할 때 아래 체크리스트를 확인하세요.

- [ ] `AcceptanceTest` 기본 클래스를 상속받았는가?
- [ ] `@ActiveProfiles("test")`가 적용되어 있는가?
- [ ] `@DisplayName`에 테스트 설명이 한글로 작성되었는가?
- [ ] `given-when-then` 주석이 모든 테스트에 포함되어 있는가?
- [ ] 헬퍼 메소드가 한글로 명확하게 명명되었는가?
- [ ] 테스트 간 데이터 격리가 보장되는가? (DatabaseCleaner 사용)
- [ ] 관련 인수 조건이 Javadoc에 명시되어 있는가?
