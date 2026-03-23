# MeeTeam 알림 시스템 분석 문서

## 목차
1. [시스템 개요](#1-시스템-개요)
2. [파일 구조](#2-파일-구조)
3. [핵심 컴포넌트](#3-핵심-컴포넌트)
4. [알림 처리 흐름](#4-알림-처리-흐름)
5. [재전송 메커니즘](#5-재전송-메커니즘)
6. [Redis 데이터 구조](#6-redis-데이터-구조)
7. [수정해야 할 부분](#7-수정해야-할-부분)
8. [리팩토링해야 할 부분](#8-리팩토링해야-할-부분)
9. [면접 대비 - 왜 SSE를 선택했는가?](#9-면접-대비---왜-sse를-선택했는가)

---

## 1. 시스템 개요

MeeTeam의 알림 시스템은 **SSE(Server-Sent Events) 기반 실시간 알림**을 구현하며, **동기 저장 + 비동기 전송** 패턴을 사용합니다.

### 핵심 설계 원칙
- 알림 데이터는 메인 비즈니스 트랜잭션 내에서 **동기적으로 DB에 저장**
- SSE 전송은 트랜잭션 커밋 후 **비동기로 처리**
- 재연결 시 미수신 이벤트를 **Redis에서 자동으로 재전송**

### 사용 기술
| 기술 | 용도 |
|------|------|
| Spring SSE (SseEmitter) | 실시간 이벤트 스트리밍 |
| Redis ZSET | 이벤트 캐시 및 순서 보장 |
| Spring Events | 이벤트 기반 비동기 처리 |
| @TransactionalEventListener | 트랜잭션 커밋 후 처리 보장 |

---

## 2. 파일 구조

```
domain/notification/
├── entity/
│   ├── Notification.java              # 알림 JPA 엔티티
│   └── NotificationType.java          # 알림 타입 enum
├── NotificationEvent.java             # 단일 수신자 이벤트
├── ProjectEndEvent.java               # 다중 수신자 이벤트 (프로젝트 종료)
├── NotificationListener.java          # @Async 이벤트 리스너
├── repository/
│   ├── NotificationRepository.java    # JPA Repository
│   ├── EmitterRepository.java         # Emitter 관리 인터페이스
│   └── EmitterRepositoryImpl.java     # Redis 기반 구현
└── service/
    ├── NotificationService.java           # 알림 조회 서비스
    ├── SSENotificationService.java        # SSE 전송 서비스
    └── NotificationSubscribeService.java  # SSE 구독 서비스

web/notification/
├── controller/
│   └── NotificationController.java    # REST API
├── context/
│   └── NotificationContext.java       # 알림 생성 컨텍스트
├── payload/
│   ├── Payload.java                   # Payload 인터페이스
│   ├── ProjectApplicationReceivedPayload.java
│   ├── ProjectApplicationApprovedPayload.java
│   ├── ProjectApplicationRejectedPayload.java
│   ├── ProjectApplicationSubmittedPayload.java
│   ├── ProjectEndedPayload.java
│   └── SimpleMessagePayload.java
├── strategy/
│   ├── NotificationPayloadStrategy.java
│   ├── ProjectApplyPayloadStrategy.java
│   ├── ProjectApplicationApprovedPayloadStrategy.java
│   ├── ProjectApplicationRejectedPayloadStrategy.java
│   ├── ApplySelfApplyPayloadStrategy.java
│   └── ProjectEndedPayloadStrategy.java
├── factory/
│   └── NotificationPayloadFactory.java
├── dto/response/
│   ├── NotificationResponse.java
│   ├── NotificationUnreadCountResponse.java
│   └── NotificationPayload.java
├── SseEnvelope.java
└── ApprovalResult.java
```

---

## 3. 핵심 컴포넌트

### 3.1 Notification 엔티티
```java
@Entity
public class Notification extends BaseEntity {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member receiver;           // 알림 수신자

    private Long actorId;              // 행위자 ID (지원자 등)
    private Long applicationId;        // 지원서 ID

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    private boolean isRead;
}
```

### 3.2 NotificationType
```java
public enum NotificationType {
    PROJECT_APPLY(true),          // 팀장에게 지원 알림 (actor 필요)
    PROJECT_MY_APPLY(false),      // 지원자에게 지원 완료 알림
    PROJECT_APPROVE(false),       // 승인 알림
    PROJECT_REJECT(false),        // 거절 알림
    PROJECT_END(false);           // 프로젝트 종료 알림

    private final boolean requiresActor;
}
```

### 3.3 이벤트 클래스
```java
// 단일 수신자 이벤트
@Data
public class NotificationEvent {
    private Long receiverId;
    private Long projectId;
    private Long actorId;
    private NotificationType type;
    private Long applicationId;
}

// 다중 수신자 이벤트
@Data
public class ProjectEndEvent {
    private NotificationType type;
    private List<Long> projectMembersId;
    private Long projectId;
    private String projectName;
    private LocalDate occurredAt;
}
```

### 3.4 Strategy 패턴 (Payload 생성)
```java
// Factory
@Component
public class NotificationPayloadFactory {
    private final Map<NotificationType, NotificationPayloadStrategy> strategies;

    public Payload create(NotificationType type, NotificationContext context) {
        return strategies.get(type).create(context);
    }
}

// Strategy 인터페이스
public interface NotificationPayloadStrategy {
    NotificationType getType();
    Payload create(NotificationContext context);
}
```

---

## 4. 알림 처리 흐름

### 4.1 전체 흐름도
```
┌─────────────────────────────────────────────────────────────────────┐
│                    메인 서비스 트랜잭션                              │
├─────────────────────────────────────────────────────────────────────┤
│ 1. 비즈니스 로직 처리 (지원서 저장 등)                               │
│ 2. Notification 엔티티 DB 저장 (동기)                               │
│ 3. NotificationEvent 발행                                           │
│ 4. COMMIT                                                           │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼ AFTER_COMMIT
┌─────────────────────────────────────────────────────────────────────┐
│              비동기 스레드 (@Async + 새 트랜잭션)                    │
├─────────────────────────────────────────────────────────────────────┤
│ 1. NotificationListener가 이벤트 수신                               │
│ 2. SSENotificationService.notify() 호출                             │
│ 3. Member, Project 엔티티 조회                                       │
│ 4. NotificationContext 생성                                         │
│ 5. PayloadFactory로 Payload 생성                                    │
│ 6. Redis에 이벤트 캐시 저장                                          │
│ 7. 활성 Emitter에 브로드캐스트                                       │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        클라이언트                                    │
├─────────────────────────────────────────────────────────────────────┤
│ EventSource로 SSE 이벤트 수신                                        │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 코드 흐름 예시 (프로젝트 지원)

**1단계: 서비스에서 알림 저장 및 이벤트 발행**
```java
// ProjectApplicationServiceImpl.apply()
public ApplicationResponse apply(Long projectId, Long memberId, ApplicationRequest request) {
    // 비즈니스 로직
    ProjectApplication savedApplication = applicationRepository.save(application);

    // 팀장용 알림 저장 (동기)
    Notification applyNotification = createNotification(
        project.getCreator(), project, actorId,
        NotificationType.PROJECT_APPLY, savedApplication.getId()
    );
    notificationRepository.save(applyNotification);

    // 지원자용 알림 저장 (동기)
    Notification myApplyNotification = createNotification(
        member, project, actorId,
        NotificationType.PROJECT_MY_APPLY, savedApplication.getId()
    );
    notificationRepository.save(myApplyNotification);

    // SSE 전송 이벤트 발행 (비동기)
    eventPublisher.publishEvent(new NotificationEvent(...));
    eventPublisher.publishEvent(new NotificationEvent(...));
}
```

**2단계: 이벤트 리스너에서 비동기 처리**
```java
// NotificationListener.java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(NotificationEvent e) {
    notificationService.notify(e);
}
```

**3단계: SSE 전송**
```java
// SSENotificationService.notify()
@Transactional
public void notify(NotificationEvent event) {
    // 엔티티 조회 및 컨텍스트 생성
    NotificationContext context = NotificationContext.of(event, project, actor);

    // Payload 생성 및 전송
    Payload payload = payloadFactory.create(type, context);
    broadcastToReceiver(receiver.getId(), type, payload);
}
```

---

## 5. 재전송 메커니즘

### 5.1 Event ID 구조
```
eventId = "{memberId}_{timestamp}"
예: "5_1708934400000"
```

### 5.2 재연결 흐름
```
1. 클라이언트 연결 끊김 (네트워크 오류, 탭 새로고침 등)
         │
         ▼
2. 클라이언트 재연결 요청
   Header: Last-Event-ID: "5_1708934400000"
         │
         ▼
3. NotificationSubscribeService.subscribe()
   ├─ lastEventId에서 timestamp 추출: 1708934400000
   ├─ Redis ZSET rangeByScore(afterTs+1, +∞) 실행
   └─ 미수신 이벤트 순서대로 재전송
         │
         ▼
4. 클라이언트가 손실된 이벤트 수신
```

### 5.3 재전송 코드
```java
// NotificationSubscribeService.subscribe()
public SseEmitter subscribe(String email, String lastEventId) {
    SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

    // PING 전송 (503 에러 방지)
    sendPing(emitter);

    // 재연결 시 미수신 이벤트 재전송
    if (hasText(lastEventId)) {
        long afterTs = extractTs(lastEventId);
        Map<String, Object> cachedEvents =
            emitterRepository.findEventCacheAfterByMemberId(memberId, afterTs);

        cachedEvents.forEach((eid, payload) ->
            sendReplay(emitter, eid, payload));
    }

    return emitter;
}
```

### 5.4 재전송(Replay) vs 재시도(Retry) - 핵심 차이

두 개념은 완전히 다른 목적과 시점에서 동작합니다.

#### 비교표

| 구분 | 재전송 (Replay) | 재시도 (Retry) |
|------|----------------|----------------|
| **시점** | 클라이언트가 **재연결할 때** | 서버가 **전송하는 순간** |
| **트리거** | 클라이언트 → 서버 (Last-Event-ID 헤더) | 서버 내부 (IOException 발생) |
| **대상** | 연결 끊긴 동안 놓친 **여러 이벤트** | 지금 보내려는 **단일 이벤트** |
| **저장소** | Redis 캐시 필요 | 저장소 불필요 |
| **구현** | `findEventCacheAfterByMemberId()` | `@Retryable` 어노테이션 |

#### 시각적 비교

```
┌─────────────────────────────────────────────────────────────────────┐
│                    재전송 (Replay) - Last-Event-ID 기반             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  시간 →                                                             │
│  ────────────────────────────────────────────────────────────────   │
│                                                                     │
│  T1: 이벤트A 전송 ✅ (클라이언트 수신)                               │
│  T2: 이벤트B 전송 ✅ (클라이언트 수신)                               │
│  T3: ──── 네트워크 끊김 ────                                        │
│  T4: 이벤트C 전송 시도 ❌ (클라이언트 없음, Redis에 캐시)            │
│  T5: 이벤트D 전송 시도 ❌ (클라이언트 없음, Redis에 캐시)            │
│  T6: ──── 클라이언트 재연결 ────                                    │
│      Header: Last-Event-ID: "이벤트B의 ID"                          │
│  T7: Redis에서 이벤트C, D 조회 → 재전송 ✅                          │
│                                                                     │
│  목적: 연결 끊긴 "동안" 놓친 이벤트들을 복구                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    재시도 (Retry) - @Retryable 기반                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  시간 →                                                             │
│  ────────────────────────────────────────────────────────────────   │
│                                                                     │
│  T1: 이벤트A 전송 시도                                              │
│      └─ 1차 시도: IOException 발생 ❌                               │
│      └─ (1초 대기)                                                  │
│      └─ 2차 시도: IOException 발생 ❌                               │
│      └─ (1초 대기)                                                  │
│      └─ 3차 시도: 성공 ✅                                           │
│                                                                     │
│  목적: 전송 "순간"의 일시적 실패를 극복                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### 실제 사용 시나리오

**재전송 (Replay)이 필요한 경우:**
```
사용자가 지하철에서 와이파이 끊김
→ 3분 동안 알림 3개 발생 (서버는 Redis에 캐시)
→ 와이파이 복구, EventSource 자동 재연결
→ Last-Event-ID 헤더로 마지막 받은 이벤트 전송
→ 서버가 Redis에서 그 이후 이벤트 3개 조회 후 재전송
```

**재시도 (Retry)가 필요한 경우:**
```
서버가 이벤트 전송 시도
→ 일시적인 네트워크 버퍼 문제로 IOException 발생
→ 1초 후 재시도 → 성공
(연결은 살아있고, 잠깐의 전송 실패만 발생한 경우)
```

#### 현재 시스템 상태

| 기능 | 구현 여부 | 설명 |
|------|----------|------|
| 재전송 (Replay) | ✅ 구현됨 | Redis 캐시 + Last-Event-ID |
| 재시도 (Retry) | ❌ 미구현 | 전송 실패 시 바로 포기 |

#### 재시도 (Retry) 구현 방법 (선택적)

```java
// 현재 코드 (재시도 없음)
emitters.forEach((emitterId, emitter) -> {
    try {
        emitter.send(event);  // 실패하면 바로 포기
    } catch (IOException e) {
        emitter.completeWithError(e);
    }
});

// 재시도 추가 시 (@Retryable 사용)
@Retryable(
    value = IOException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
private void sendWithRetry(SseEmitter emitter, SseEmitter.SseEventBuilder event)
        throws IOException {
    emitter.send(event);
}
```

> **참고:** 재시도(Retry)는 선택적 구현입니다.
> - 이미 재전송(Replay)이 구현되어 있어서, 전송 실패해도 재연결 시 복구됨
> - 재시도는 "즉시 전달"이 중요할 때 추가하면 좋음
> - 대부분의 SSE 시스템은 재전송만으로 충분함

---

## 6. Redis 데이터 구조

### 6.1 저장 구조
```
┌─────────────────────────────────────────────────────────────────────┐
│ eventCache:5 (ZSET) - TTL: 2시간                                    │
├─────────────────────────────────────────────────────────────────────┤
│ Member (eventId)          │ Score (timestamp)                       │
│ "5_1708934400000"         │ 1708934400000                           │
│ "5_1708934401000"         │ 1708934401000                           │
│ "5_1708934402000"         │ 1708934402000                           │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ eventCache:evt:5_1708934400000 (String) - TTL: 2시간                │
├─────────────────────────────────────────────────────────────────────┤
│ {"type":"PROJECT_APPLY", "data":{...}, "createdAt":"2024-02-26"}   │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 TTL 정책
| 키 타입 | TTL | 설명 |
|---------|-----|------|
| eventCache:{memberId} (ZSET) | 2시간 | 이벤트 인덱스 |
| eventCache:evt:{eventId} (String) | 2시간 | 실제 이벤트 데이터 |

> **TTL 2시간 설정 이유:**
> - SSE 타임아웃(1시간) + 네트워크 장애 여유(1시간)
> - 대부분의 네트워크 장애는 2시간 내 복구됨
> - 2시간 넘게 오프라인이면 사용자가 새로고침할 가능성 높음
| eventCache:{memberId} (ZSET) | 7일 | 이벤트 인덱스 |
| eventCache:evt:{eventId} (String) | 1일 | 실제 이벤트 데이터 |

---

## 7. 수정해야 할 부분

### 7.1 [Critical] ProjectEndEvent에서 DB 저장 중복 문제 - ✅ 해결됨

**해결 방법:**
`NotificationSaveService` 클래스를 신설하여 모든 알림 저장을 서비스 레이어에서 통합 관리하도록 수정했습니다.

- `NotificationSaveService`: 알림 저장 전용 서비스 (신설)
- `ProjectServiceImpl.deleteProject()`: 프로젝트 삭제 시 `notificationSaveService.saveForProjectEnd()` 호출
- `ProjectApplicationServiceImpl`: `notificationSaveService.saveForApply()`, `saveForApprove()`, `saveForReject()` 사용
- `SSENotificationService`: DB 저장 로직 제거, SSE 전송만 담당

### 7.2 [Critical] ZSET과 이벤트 데이터 TTL 불일치 - ✅ 해결됨

**해결 방법:**
ZSET 인덱스와 이벤트 데이터의 TTL을 **2시간**으로 통일했습니다.

```java
// EmitterRepositoryImpl.java
private static final Duration EVENT_CACHE_TTL = Duration.ofHours(2);
```

**2시간으로 설정한 이유:**
- SSE 타임아웃(1시간) + 네트워크 장애 여유(1시간)
- 대부분의 네트워크 장애는 2시간 내 복구됨
- 2시간 넘게 오프라인이면 사용자가 새로고침할 가능성 높음
- Redis 메모리 효율적 사용

### 7.3 [High] 에러 핸들링 개선

**현재 문제:**
SSE 전송 실패 시 로그만 남기고 있음. 재시도 로직 없음.

```java
} catch (Exception e) {
    log.error("[알림] 전송 실패 (DB 저장은 완료됨) - ...");
    // 재시도 없음
}
```

**수정 방안:**
```java
// 재시도 로직 추가
@Retryable(value = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
private void sendWithRetry(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
    emitter.send(event);
}
```

### 7.4 [Medium] Emitter 메모리 관리 개선

**현재 문제:**
- Emitter는 ConcurrentHashMap에 저장 (메모리)
- 서버 재시작 시 모든 연결 손실
- 멀티 인스턴스 환경에서 다른 서버의 Emitter에 접근 불가

**수정 방안:**
멀티 인스턴스 지원을 위해 Redis Pub/Sub 추가:
```java
// 이벤트 발생 시 Redis Pub/Sub으로 브로드캐스트
redisTemplate.convertAndSend("notification-channel", envelope);

// 각 서버 인스턴스에서 구독하여 자신의 Emitter에 전송
@RedisListener(topics = "notification-channel")
public void onMessage(SseEnvelope envelope) {
    broadcastToLocalEmitters(envelope);
}
```

### 7.5 [Medium] 타임아웃 설정 하드코딩

**현재 문제:**
```java
private static final long DEFAULT_TIMEOUT = 60L * 60L * 1000L; // 1시간
```

**수정 방안:**
```java
@Value("${notification.sse.timeout:3600000}")
private long sseTimeout;
```

---

## 8. 리팩토링해야 할 부분

### 8.1 [High] 알림 저장 로직 분리 및 통일 - ✅ 완료됨

**해결됨:** `NotificationSaveService` 클래스가 신설되어 모든 알림 저장을 통합 관리합니다.

**구현된 메서드:**
- `save()`: 단일 알림 저장
- `saveForProjectEnd()`: 프로젝트 종료 알림 일괄 저장
- `saveForApply()`: 지원 알림 저장 (팀장 + 지원자)
- `saveForApprove()`: 승인 알림 저장
- `saveForReject()`: 거절 알림 저장

### 8.2 [High] 이벤트 클래스 통합

**현재 문제:**
`NotificationEvent`와 `ProjectEndEvent`가 별도로 존재하여 리스너도 2개 필요.

**리팩토링 방안:**
```java
// 단일 이벤트 클래스로 통합
@Data
@Builder
public class NotificationEvent {
    private NotificationType type;
    private List<Long> receiverIds;  // 단일 수신자도 List로 통일
    private Long projectId;
    private Long actorId;
    private Long applicationId;
    private String projectName;
    private LocalDate occurredAt;

    // 단일 수신자용 팩토리
    public static NotificationEvent single(Long receiverId, ...) {
        return NotificationEvent.builder()
            .receiverIds(List.of(receiverId))
            ...
            .build();
    }

    // 다중 수신자용 팩토리
    public static NotificationEvent multiple(List<Long> receiverIds, ...) {
        return NotificationEvent.builder()
            .receiverIds(receiverIds)
            ...
            .build();
    }
}
```

### 8.3 [Medium] EmitterRepository 인터페이스 정리

**현재 문제:**
메서드명이 직관적이지 않고 일관성이 부족함.

**리팩토링 방안:**
```java
public interface EmitterRepository {
    // Emitter 관리
    void saveEmitter(Long memberId, SseEmitter emitter);
    void deleteEmitter(Long memberId);
    Map<String, SseEmitter> findEmittersByMemberId(Long memberId);

    // 이벤트 캐시 관리
    void cacheEvent(Long memberId, long timestamp, Object event);
    List<CachedEvent> findEventsAfter(Long memberId, long afterTimestamp);
    void clearEventCache(Long memberId);
}

// CachedEvent record 추가
public record CachedEvent(String eventId, Object payload, long timestamp) {}
```

### 8.4 [Medium] NotificationContext 빌더 패턴 개선

**현재 문제:**
`NotificationContext.of()` 정적 메서드들이 많아서 복잡함.

**리팩토링 방안:**
```java
// Builder 패턴 사용
NotificationContext context = NotificationContext.builder()
    .receiverId(receiverId)
    .projectId(projectId)
    .projectName(projectName)
    .actorId(actorId)
    .actorName(actorName)
    .applicationId(applicationId)
    .occurredAt(LocalDate.now())
    .build();
```

### 8.5 [Medium] Payload 생성 로직 간소화

**현재 문제:**
각 Payload마다 Strategy 클래스가 필요하여 파일이 많음.

**리팩토링 방안:**
```java
// 람다 기반 등록
@Component
public class NotificationPayloadFactory {

    private final Map<NotificationType, Function<NotificationContext, Payload>> creators;

    @PostConstruct
    public void init() {
        creators = Map.of(
            PROJECT_APPLY, ProjectApplicationReceivedPayload::create,
            PROJECT_MY_APPLY, ProjectApplicationSubmittedPayload::create,
            PROJECT_APPROVE, ctx -> ProjectApplicationApprovedPayload.create(ctx, APPROVED),
            PROJECT_REJECT, ctx -> ProjectApplicationRejectedPayload.create(ctx, REJECTED),
            PROJECT_END, ProjectEndedPayload::create
        );
    }

    public Payload create(NotificationType type, NotificationContext context) {
        return creators.get(type).apply(context);
    }
}
```

### 8.6 [Low] 상수 분리

**현재 문제:**
매직 넘버와 문자열이 코드에 하드코딩됨.

**리팩토링 방안:**
```java
public class NotificationConstants {
    public static final long DEFAULT_SSE_TIMEOUT_MS = 60 * 60 * 1000L;
    public static final Duration EVENT_CACHE_TTL = Duration.ofDays(1);
    public static final Duration ZSET_TTL = Duration.ofDays(1);
    public static final String REDIS_EVENT_CACHE_PREFIX = "eventCache:";
    public static final String REDIS_EVENT_DATA_PREFIX = "eventCache:evt:";
    public static final String PING_EVENT_NAME = "PING";
}
```

### 8.7 [Low] 예외 클래스 세분화

**현재 문제:**
모든 에러가 `CustomException(ErrorCode.XXX)` 형태로 처리됨.

**리팩토링 방안:**
```java
// 알림 도메인 전용 예외
public class NotificationException extends CustomException {
    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }
}

public class SseConnectionException extends NotificationException {
    public SseConnectionException() {
        super(ErrorCode.SSE_CONNECTION_FAILED);
    }
}
```

---

## 부록: 우선순위별 작업 목록

### 즉시 수정 필요 (Critical)
1. ProjectEndEvent DB 저장 위치 통일
2. Redis TTL 불일치 수정

### 조기 수정 권장 (High)
3. 알림 저장 로직 분리 (NotificationSaveService)
4. 이벤트 클래스 통합
5. 에러 핸들링 및 재시도 로직 추가

### 중기 개선 (Medium)
6. EmitterRepository 인터페이스 정리
7. 멀티 인스턴스 지원 (Redis Pub/Sub)
8. 설정값 외부화 (@Value)
9. NotificationContext 빌더 패턴

### 장기 개선 (Low)
10. 상수 분리
11. Payload Strategy 간소화
12. 예외 클래스 세분화

---

## 참고: 현재 API 엔드포인트

| 메서드 | 엔드포인트 | 설명 |
|--------|----------|------|
| GET | `/api/subscribe` | SSE 구독 (Last-Event-ID 헤더 지원) |
| GET | `/api/notifications` | 알림 목록 조회 (페이징) |
| GET | `/api/notifications/unread/count` | 미읽음 개수 조회 |

---

## 9. 면접 대비 - 왜 SSE를 선택했는가?

### 9.1 핵심 답변 (30초 버전)

> "알림 시스템은 **서버 → 클라이언트 단방향 통신**이 필요합니다.
> WebSocket은 양방향이라 오버헤드가 크고, Polling은 불필요한 요청이 많습니다.
> SSE는 **단방향 실시간 푸시에 최적화**되어 있고, **HTTP 기반이라 기존 인프라를 그대로 사용**할 수 있어서 선택했습니다."

### 9.2 실시간 통신 방식 비교

```
┌─────────────────────────────────────────────────────────────────────┐
│                    실시간 통신 방식 비교                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   Polling              WebSocket              SSE                   │
│   ────────             ─────────              ───                   │
│                                                                     │
│   클라 → 서버          클라 ↔ 서버            클라 ← 서버           │
│   "새거 있어?"          양방향                 단방향 푸시           │
│   "없어"                                                            │
│   "새거 있어?"                                                      │
│   "없어"                                                            │
│   ...                                                               │
│                                                                     │
│   ❌ 불필요한 요청      ❌ 과한 스펙           ✅ 딱 맞음            │
│   ❌ 서버 부하          ❌ 복잡한 구현          ✅ 단순함             │
│   ❌ 지연 발생          ✅ 실시간              ✅ 실시간             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.3 SSE를 선택한 이유 (구체적)

| 기준 | SSE | WebSocket | 선택 이유 |
|------|-----|-----------|----------|
| **통신 방향** | 단방향 (서버→클라) | 양방향 | 알림은 서버가 푸시만 하면 됨 |
| **프로토콜** | HTTP | WS (별도 프로토콜) | 기존 인프라(로드밸런서, 프록시) 호환 |
| **재연결** | 브라우저가 자동 처리 | 직접 구현해야 함 | 개발 복잡도 낮음 |
| **배터리** | 효율적 | 상대적으로 비효율 | 모바일 환경 고려 |
| **구현** | 단순 (EventSource API) | 복잡 (STOMP 등) | 유지보수 용이 |

### 9.4 꼬리 질문 대비

#### Q: "그럼 WebSocket은 언제 쓰나요?"

> "채팅처럼 **클라이언트도 서버로 메시지를 보내야 하는 경우**에 WebSocket을 씁니다.
> 실제로 저희 프로젝트에서도 **채팅은 WebSocket(STOMP)**, **알림은 SSE**로 분리해서 사용합니다.
> 각 기술의 특성에 맞게 선택했습니다."

```
┌─────────────────────────────────────────────────────────────────────┐
│                  우리 프로젝트 실시간 통신 구조                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   채팅 (양방향)                    알림 (단방향)                    │
│   ─────────────                    ─────────────                    │
│   클라 ↔ 서버                      클라 ← 서버                      │
│   "안녕" →                         ← "지원서 도착"                  │
│   ← "응 안녕"                      ← "승인됨"                       │
│   "뭐해?" →                                                         │
│                                                                     │
│   → WebSocket (STOMP + RabbitMQ)   → SSE (SseEmitter)              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### Q: "SSE의 단점은요?"

> "브라우저당 **동시 연결 수 제한**이 있습니다 (HTTP/1.1 기준 도메인당 6개).
> 하지만 HTTP/2에서는 멀티플렉싱으로 해결되고,
> 알림은 탭당 1개 연결이면 충분해서 실제로 문제가 되지 않았습니다."

#### Q: "연결이 끊기면 어떻게 하나요?"

> "SSE의 장점 중 하나가 **브라우저의 EventSource API가 자동으로 재연결**을 시도한다는 점입니다.
> 하지만 단순 재연결만으로는 **연결이 끊긴 동안 발생한 알림을 놓치는 문제**가 있습니다.
>
> 이를 해결하기 위해 **3가지 메커니즘**을 구현했습니다:
>
> 1. **이벤트 ID 설계**: 모든 이벤트에 `{memberId}_{timestamp}` 형식의 고유 ID를 부여합니다.
>
> 2. **Redis 캐시**: 이벤트 전송 시 Redis에도 저장합니다. ZSET에는 이벤트 ID를 timestamp를 score로 저장하고, String에는 실제 이벤트 데이터를 저장합니다. TTL은 2시간으로 설정했습니다.
>
> 3. **Last-Event-ID 활용**: 브라우저가 재연결할 때 HTTP 헤더에 마지막으로 받은 이벤트 ID를 자동으로 포함합니다. 서버는 이 ID에서 timestamp를 추출하고, Redis ZSET의 `rangeByScore`로 그 이후의 이벤트만 조회해서 재전송합니다.
>
> 이렇게 하면 네트워크가 잠깐 끊겼다가 복구되어도 **알림 손실 없이 모든 이벤트를 순서대로 받을 수 있습니다.**"

```
재연결 흐름 상세:

1. 정상 연결 중
   서버 → 클라이언트: id="5_1708934400000", data={알림A}
   서버 → 클라이언트: id="5_1708934401000", data={알림B}
   (클라이언트는 마지막 ID "5_1708934401000"을 기억)

2. 네트워크 끊김 (T+0)
   └─ 서버: Emitter 에러 감지, 정리
   └─ 클라이언트: EventSource가 연결 끊김 감지

3. 끊긴 동안 알림 발생 (T+30초)
   └─ 서버: 알림C 발생
   └─ 서버: DB 저장 ✅, Redis 캐시 ✅, SSE 전송 ❌ (연결 없음)

4. 자동 재연결 시도 (T+3초, T+6초, ...)
   └─ EventSource가 자동으로 재연결 시도
   └─ 요청 헤더: Last-Event-ID: "5_1708934401000"

5. 서버 처리
   └─ timestamp 추출: 1708934401000
   └─ Redis ZSET rangeByScore(1708934401001, +∞) 실행
   └─ 결과: 알림C (놓친 이벤트)
   └─ 알림C 재전송

6. 클라이언트
   └─ 놓친 알림C 수신 ✅
   └─ 이후 실시간 알림 정상 수신
```

**핵심 포인트:**
- 서버는 "재연결인지 새 연결인지" 판단하지 않음
- Last-Event-ID 헤더가 있으면 재전송, 없으면 새 연결로 처리
- Redis ZSET의 score 기반 범위 검색으로 O(log N) 시간복잡도

#### Q: "서버가 여러 대면 어떻게 하나요?" (확장성)

> "현재는 Emitter를 인메모리(ConcurrentHashMap)에 저장하고 있어서
> 멀티 인스턴스 환경에서는 한계가 있습니다.
> 확장 시에는 **Redis Pub/Sub**을 추가해서 서버 간 이벤트 브로드캐스트를 구현할 계획입니다."

```
현재: 단일 인스턴스
┌─────────┐
│ Server  │ ← Emitter 메모리 저장
└─────────┘

확장 시: Redis Pub/Sub 추가
┌─────────┐     ┌─────────┐
│ Server1 │ ←─→ │  Redis  │ ←─→ │ Server2 │
└─────────┘     │ Pub/Sub │     └─────────┘
                └─────────┘
```

### 9.5 한 문장 정리

> **"알림은 서버가 클라이언트에게 일방적으로 푸시하는 구조라서,
> 양방향인 WebSocket보다 단방향에 최적화된 SSE가 더 적합합니다."**
