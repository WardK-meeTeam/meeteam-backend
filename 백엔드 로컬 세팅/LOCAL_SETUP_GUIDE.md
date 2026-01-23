# 로컬 환경 실행 가이드

## 발생했던 문제

로컬에서 `./gradlew bootRun` 실행 시 애플리케이션이 시작되지 않는 문제가 발생했습니다.

### 에러 로그
```
APPLICATION FAILED TO START

Description:
Web server failed to start. Port 8080 was already in use.

또는

TCP connection failure in session _system_: Failed to connect: null
```

## 원인 분석

### 1. 포트 8080 충돌
이전에 실행된 Java 프로세스가 종료되지 않아 포트 8080이 이미 사용 중이었습니다.

### 2. Redis 미실행
세션 관리 및 캐싱을 위한 Redis가 실행되지 않았습니다.
- 필요 포트: `6379`

### 3. RabbitMQ 미실행
WebSocket STOMP 통신을 위한 RabbitMQ가 실행되지 않았습니다.
- AMQP 포트: `5672`
- STOMP 포트: `61613`
- 관리 콘솔: `15672`

## 해결 방법

### Step 1. 포트 8080 점유 프로세스 확인 및 종료

```bash
# 포트 8080 사용 중인 프로세스 확인
lsof -i :8080

# 프로세스 종료 (PID는 위 명령어 결과에서 확인)
kill <PID>
```

### Step 2. Docker로 인프라 실행

#### Redis 시작
```bash
docker run -d \
  --name meeteam-redis \
  -p 6379:6379 \
  redis:7.0-alpine
```

#### RabbitMQ 시작 (STOMP 플러그인 포함)
```bash
# RabbitMQ 컨테이너 시작
docker run -d \
  --name meeteam-rabbitmq \
  -p 5672:5672 \
  -p 61613:61613 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management

# STOMP 플러그인 활성화 (컨테이너 시작 후 10초 대기)
sleep 10
docker exec meeteam-rabbitmq rabbitmq-plugins enable rabbitmq_stomp
```

### Step 3. 애플리케이션 실행

```bash
# local 프로파일로 실행
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

## 빠른 시작 (이미 컨테이너가 생성된 경우)

```bash
# 1. 인프라 시작
docker start meeteam-redis meeteam-rabbitmq

# 2. 애플리케이션 실행
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

## 인프라 상태 확인

```bash
# Docker 컨테이너 상태 확인
docker ps

# 포트 사용 현황 확인
lsof -i :8080   # Spring Boot
lsof -i :6379   # Redis
lsof -i :5672   # RabbitMQ AMQP
lsof -i :61613  # RabbitMQ STOMP

# 애플리케이션 헬스체크
curl http://localhost:8080/actuator/health
```

## 필수 인프라 요약

| 서비스 | 포트 | 용도 |
|--------|------|------|
| MySQL | 3307 | 메인 데이터베이스 |
| Redis | 6379 | 세션/캐시 |
| RabbitMQ | 5672, 61613 | 메시지 브로커, WebSocket STOMP |

## 트러블슈팅

### Q. `Port 8080 was already in use` 에러
```bash
# 강제 종료
kill -9 $(lsof -t -i:8080)
```

### Q. Redis 연결 실패
```bash
# Redis 컨테이너 로그 확인
docker logs meeteam-redis

# 재시작
docker restart meeteam-redis
```

### Q. RabbitMQ STOMP 연결 실패
```bash
# STOMP 플러그인 활성화 확인
docker exec meeteam-rabbitmq rabbitmq-plugins list | grep stomp

# 플러그인이 비활성화된 경우 활성화
docker exec meeteam-rabbitmq rabbitmq-plugins enable rabbitmq_stomp
```

---

# 테스트 실행 가이드

## 테스트 실행이 안 됐던 문제

`./gradlew test` 실행 시 Spring Context 로드 실패로 테스트가 실행되지 않는 문제가 발생했습니다.

### 에러 로그
```
PlaceholderResolutionException: Could not resolve placeholder 'file.upload.s3.base-url'
NullPointerException at SecurityUrls.java:51
NoSuchBeanDefinitionException: ClientRegistrationRepository
```

## 원인 분석

`application-test.yml`에 필요한 설정들이 누락되어 있었습니다.

| 누락된 설정 | 사용처 | 설명 |
|------------|--------|------|
| `file.upload.s3.base-url` | S3FileService | S3 파일 업로드 URL |
| `file.dir` | FileUtil | 로컬 파일 저장 경로 |
| `security.whitelist` | SecurityUrls | 인증 없이 접근 가능한 URL 목록 |
| `app.oauth2.*` | OAuth2Properties | OAuth2 리다이렉트 및 프로바이더 설정 |
| `cors.*` | CorsProperties | CORS 설정 |
| `spring.security.oauth2.client.*` | ClientRegistrationRepository | OAuth2 클라이언트 등록 정보 |
| `github.*` | GithubAppAuthService | GitHub App 인증 설정 |

### DatabaseCleaner 문제

`@Table(name="")` 또는 `@Table` 어노테이션이 없는 엔티티의 경우 테이블명을 가져오지 못해 `TRUNCATE TABLE` 실행 시 에러가 발생했습니다.

```
SQL Error: Syntax error in SQL statement "TRUNCATE TABLE [*]"; expected "identifier"
```

## 해결 방법

### 1. application-test.yml 설정 추가

`src/test/resources/application-test.yml`에 아래 설정들이 추가되었습니다:

```yaml
spring:
  # OAuth2 클라이언트 등록 (테스트용 더미 값)
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test-google-client-id
            client-secret: test-google-client-secret
          github:
            client-id: test-github-client-id
            client-secret: test-github-client-secret

file:
  dir: /tmp/meeteam-test
  upload:
    s3:
      base-url: https://test-bucket.s3.us-east-1.amazonaws.com

security:
  whitelist:
    - method: "*"
      uri: /api/auth/**
    # ... 기타 화이트리스트

app:
  oauth2:
    oauth2-redirect-url: http://localhost:3000/oauth2/redirect
    # ... 프로바이더 설정

cors:
  allowed-origins:
    - http://localhost:3000
  # ... 기타 CORS 설정

github:
  app:
    id: 12345
    private-key-pem: |
      -----BEGIN PRIVATE KEY-----
      (테스트용 더미 키)
      -----END PRIVATE KEY-----
  webhook:
    secret: test-webhook-secret
```

### 2. DatabaseCleaner 수정

`@Table` 어노테이션이 없거나 `name`이 비어있는 엔티티의 경우 엔티티 이름을 snake_case로 변환하여 테이블명으로 사용하도록 수정했습니다.

## 테스트 실행

```bash
# 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "ProjectSearchAcceptanceTest"
```

## 테스트 트러블슈팅

### Q. `PlaceholderResolutionException` 에러
설정 파일에 필요한 프로퍼티가 누락된 경우입니다. 에러 메시지에서 어떤 placeholder가 누락됐는지 확인하고 `application-test.yml`에 추가하세요.

### Q. `NoSuchBeanDefinitionException` 에러
필요한 빈이 생성되지 않은 경우입니다. 해당 빈이 의존하는 설정을 확인하세요.

### Q. `TRUNCATE TABLE` SQL 에러
DatabaseCleaner에서 테이블명을 가져오지 못한 경우입니다. 엔티티에 `@Table(name="테이블명")` 어노테이션이 제대로 설정되어 있는지 확인하세요.

---

## 테스트 실행 중 발생한 추가 문제 및 해결

### 1. "프로젝트를 찾을 수 없습니다" 에러

**원인:** `ProjectRepository.findByIdWithMembers()` 쿼리가 `JOIN FETCH`(INNER JOIN)를 사용하여 멤버가 없는 프로젝트는 조회되지 않음

**에러 로그:**
```
CustomException: 프로젝트를 찾을 수 없습니다.
at ProjectMemberServiceImpl.getProjectMembers
```

**해결:** `JOIN FETCH` → `LEFT JOIN FETCH`로 변경

```java
// 수정 전 (멤버가 없으면 프로젝트 조회 안됨)
@Query("SELECT DISTINCT p FROM Project p " +
        "JOIN FETCH p.members pm " +
        "JOIN FETCH pm.member m " +
        "WHERE p.id = :projectId AND p.isDeleted = false")

// 수정 후 (멤버가 없어도 프로젝트 조회 가능)
@Query("SELECT DISTINCT p FROM Project p " +
        "LEFT JOIN FETCH p.members pm " +
        "LEFT JOIN FETCH pm.member m " +
        "WHERE p.id = :projectId AND p.isDeleted = false")
```

**파일:** `src/main/java/.../domain/project/repository/ProjectRepository.java`

---

### 2. ProjectCounts NullPointerException

**원인:** `projectCategoryApplicationRepository.findTotalCountsByProject()`가 null 반환 시 NPE 발생

**에러 로그:**
```
NullPointerException: Cannot invoke "ProjectCounts.getCurrentCount()" because "totalCountsByProject" is null
```

**해결:** null 체크 추가

```java
// 수정 전
Long currentCount = totalCountsByProject.getCurrentCount();
Long recruitmentCount = totalCountsByProject.getRecruitmentCount();

// 수정 후
Long currentCount = totalCountsByProject != null ? totalCountsByProject.getCurrentCount() : 0L;
Long recruitmentCount = totalCountsByProject != null ? totalCountsByProject.getRecruitmentCount() : 0L;
```

**파일:** `src/main/java/.../domain/project/service/ProjectServiceImpl.java`

---

### 3. 테스트 API 엔드포인트 Whitelist 누락

**원인:** `/api/main/**` 엔드포인트가 `security.whitelist`에 없어서 인증 에러 발생

**해결:** `application-test.yml`에 whitelist 추가

```yaml
security:
  whitelist:
    - method: GET
      uri: /api/main/**
```

---

### 4. 테스트 응답 필드명 불일치

**원인:** 테스트 코드에서 `result.content[0].name`을 사용했지만 실제 응답은 `projectName`

**해결:** 테스트 코드 수정

```java
// 수정 전
assertThat(response.jsonPath().getString("result.content[0].name"))

// 수정 후
assertThat(response.jsonPath().getString("result.content[0].projectName"))
```

**파일:** `src/test/java/.../acceptance/project/ProjectSearchAcceptanceTest.java`

---

## 최종 테스트 결과

모든 인수 테스트 통과 (100% 성공)

| 테스트 클래스 | 테스트 수 | 성공률 |
|--------------|----------|--------|
| 복합_필터_검색 | 1 | 100% |
| 프로젝트_목록_조회 | 1 | 100% |
| 프로젝트_필터_검색 | 1 | 100% |
