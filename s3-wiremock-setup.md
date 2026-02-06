# S3 WireMock 인수테스트 사전 작업

## 목표

`file-upload.feature` 시나리오에서 실제 AWS S3 대신 **WireMock**으로 S3 API를 모킹하여,
외부 의존 없이 파일 업로드 인수테스트를 실행할 수 있도록 한다.

---

## 테스트 요청 흐름

인수테스트에서 파일 업로드 API를 호출했을 때의 전체 흐름이다.
**API 레이어는 실제 서버를 호출**하고, **외부 인프라(S3)만 WireMock으로 대체**한다.

```
┌─────────────┐        HTTP         ┌──────────────────────┐
│  RestAssured │ ──────────────────▶ │  Spring Boot 서버     │
│  (FileApi)   │  PUT /api/members   │  (실제 API 엔드포인트) │
└─────────────┘                     └──────────┬───────────┘
                                               │
                                        S3FileService
                                      validateFile() → 확장자/크기/빈파일 검증
                                      amazonS3.putObject()
                                               │
                                        HTTP (PUT /test-bucket/...)
                                               │
                                               ▼
                                    ┌─────────────────────┐
                                    │  WireMock            │
                                    │  (FakeS3Server)      │
                                    │                      │
                                    │  - PUT  → 200 + ETag │
                                    │  - DELETE → 204      │
                                    │  - 요청 기록 보관     │
                                    └─────────────────────┘
```

**핵심 포인트:**
- `FileApi`는 WireMock을 직접 호출하지 않는다. 실제 Spring Boot API를 호출한다.
- `TestS3Config`가 `AmazonS3` 빈의 엔드포인트를 WireMock으로 교체한다.
- `S3FileService` 입장에서는 평소와 동일하게 `amazonS3.putObject()`를 호출하지만, 목적지가 AWS가 아닌 WireMock이다.
- 검증 시 `fakeS3Server.getUploadCount()`로 실제 S3 요청 도달 여부를 확인할 수 있다.
- 검증 실패(확장자/크기/빈파일)는 `S3FileService.validateFile()`에서 걸러지므로 WireMock까지 도달하지 않는다.

---

## 현재 상태 분석

| 항목 | 현재 |
|------|------|
| S3 클라이언트 | `AmazonS3ClientBuilder.standard()` (S3Config.java) |
| 테스트 프로필 설정 | `application-test.yml`에 더미 키/버킷 존재하나 실제 S3 연결 시도 |
| 파일 업로드 서비스 | `S3FileService` → `amazonS3.putObject()` 직접 호출 |
| 파일 검증 | 확장자(jpg,png,jpeg), 크기(5MB), 빈 파일 검증 |

---

## 사전 작업 목록

### 1. WireMock 의존성 추가

`build.gradle`에 WireMock 의존성을 추가한다.

```groovy
// WireMock for S3 mocking
testImplementation 'org.wiremock:wiremock-standalone:3.3.1'
```

> WireMock 3.x는 JUnit 5와 호환되며 standalone 버전은 Jetty를 내장하고 있다.

---

### 2. 테스트용 S3Config 오버라이드

현재 `S3Config`는 `AmazonS3ClientBuilder.standard().withRegion(region)` 으로 생성하므로
실제 AWS 엔드포인트에 연결을 시도한다.

테스트 시에는 WireMock 서버의 로컬 URL을 S3 엔드포인트로 지정해야 한다.

**파일**: `src/test/java/.../acceptance/cucumber/support/TestS3Config.java`

```java
@TestConfiguration
@Profile("test")
public class TestS3Config {

    @Bean
    @Primary
    public AmazonS3 amazonS3(
            @Value("${cloud.aws.credentials.access-key}") String accessKey,
            @Value("${cloud.aws.credentials.secret-key}") String secretKey,
            @Value("${wiremock.s3.endpoint}") String endpoint
    ) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withPathStyleAccessEnabled(true)  // WireMock은 path-style 필수
                .build();
    }
}
```

핵심:
- `withEndpointConfiguration`으로 WireMock URL을 엔드포인트로 지정
- `withPathStyleAccessEnabled(true)` → WireMock이 `http://localhost:PORT/bucket/key` 형태로 요청을 받을 수 있도록 path-style 활성화
- `@Primary`로 기존 `S3Config`의 빈을 오버라이드

---

### 3. WireMock S3 서버 Support 클래스 생성

Cucumber 시나리오의 라이프사이클에 맞춰 WireMock 서버를 시작/종료하고,
S3 API에 대한 스텁을 등록하는 support 클래스를 만든다.

**파일**: `src/test/java/.../acceptance/cucumber/support/FakeS3Server.java`

```java
@Component
public class FakeS3Server {

    private WireMockServer wireMockServer;

    public void start() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .dynamicPort());
        wireMockServer.start();
        stubS3PutObject();
        stubS3DeleteObject();
    }

    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public int getPort() {
        return wireMockServer.port();
    }

    public String getEndpoint() {
        return "http://localhost:" + getPort();
    }

    /** S3 PutObject 스텁 - 모든 PUT 요청에 200 응답 */
    private void stubS3PutObject() {
        wireMockServer.stubFor(WireMock.put(WireMock.urlPathMatching("/test-bucket/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("ETag", "\"fake-etag\"")));
    }

    /** S3 DeleteObject 스텁 */
    private void stubS3DeleteObject() {
        wireMockServer.stubFor(WireMock.delete(WireMock.urlPathMatching("/test-bucket/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(204)));
    }

    /** 업로드된 파일 수 검증용 */
    public int getUploadCount() {
        return wireMockServer.findAll(
                WireMock.putRequestedFor(WireMock.urlPathMatching("/test-bucket/.*"))
        ).size();
    }

    public void reset() {
        wireMockServer.resetRequests();
    }
}
```

기존 `FakeOAuthServer` 패턴과 동일하게 `@Component`로 등록하여
Steps 클래스에서 `@Autowired`로 주입받아 사용한다.

---

### 4. application-test.yml 설정 추가

WireMock S3 엔드포인트 설정을 추가한다.
실제 포트는 `FakeS3Server`가 동적으로 할당하므로 기본값만 선언한다.

```yaml
wiremock:
  s3:
    endpoint: http://localhost:0  # FakeS3Server에서 동적 할당
```

또는 `TestS3Config`에서 `FakeS3Server`를 직접 주입받아 포트를 가져오는 방식을 사용한다:

```java
@Bean
@Primary
public AmazonS3 amazonS3(FakeS3Server fakeS3Server, ...) {
    fakeS3Server.start();
    // fakeS3Server.getEndpoint()를 엔드포인트로 사용
}
```

---

### 5. Cucumber 라이프사이클에 WireMock 연동

`CucumberSpringConfiguration`의 `@Before`에서 FakeS3Server를 리셋하고,
`@After`에서 정리한다.

```java
@CucumberContextConfiguration
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestS3Config.class)  // 추가
public class CucumberSpringConfiguration {

    @Autowired
    private FakeS3Server fakeS3Server;

    @Before
    public void setUp() {
        RestAssured.port = port;
        databaseCleaner.execute();
        fakeS3Server.reset();  // 시나리오마다 요청 기록 초기화
        // ...
    }
}
```

---

### 6. file.upload 설정값 테스트에 맞게 조정

현재 `application-test.yml`에 이미 설정이 있지만, feature 시나리오와 맞춰야 한다:

```yaml
file:
  upload:
    allowed-extensions: jpg,jpeg,png,gif,webp  # gif, webp 추가 필요
    max-size: 5MB                              # 5MB (현재 바이트 값으로 되어 있음)
    s3:
      base-url: http://localhost:{PORT}/test-bucket  # WireMock 엔드포인트
```

현재 `allowed-extensions: jpg,png,jpeg` → `gif`, `webp` 누락으로 GIF 업로드 시나리오가 실패한다.
feature에서 `animation.gif` 업로드 성공을 기대하므로 `gif,webp`를 추가해야 한다.

`max-size`는 현재 `5242880` (바이트 값)이고, `S3FileService.parseMaxSize()`가 `MB`/`KB` suffix를 파싱하므로 `5MB`로 변경하거나 바이트 파싱 로직을 추가해야 한다.

---

## 작업 순서 요약

| 순서 | 작업 | 파일 |
|------|------|------|
| 1 | WireMock 의존성 추가 | `build.gradle` |
| 2 | FakeS3Server support 클래스 생성 | `support/FakeS3Server.java` |
| 3 | 테스트용 S3Config 오버라이드 | `support/TestS3Config.java` |
| 4 | application-test.yml S3 설정 수정 | `application-test.yml` |
| 5 | CucumberSpringConfiguration에 연동 | `CucumberSpringConfiguration.java` |
| 6 | allowed-extensions에 gif, webp 추가 | `application-test.yml` |
| 7 | FileUploadSteps에서 FakeS3Server 검증 활용 | `steps/FileUploadSteps.java` |

---

## 참고: WireMock vs 다른 대안 비교

| 방식 | 장점 | 단점 |
|------|------|------|
| **WireMock** | HTTP 레벨에서 S3 API를 정확하게 모킹, 요청 검증 가능 | 설정 필요 |
| LocalStack | 실제 S3와 거의 동일한 동작 | Docker 필요, CI 환경 복잡 |
| Mockito (@MockBean) | 간단 | 인수테스트의 의도와 맞지 않음 (서비스 레이어 모킹) |
| S3Mock (Adobe) | 경량 S3 에뮬레이터 | 유지보수 중단 |

인수테스트는 실제 HTTP 요청 흐름을 검증하는 것이 목적이므로,
서비스 레이어 모킹보다 **WireMock으로 S3 HTTP API를 스텁**하는 방식이 적합하다.