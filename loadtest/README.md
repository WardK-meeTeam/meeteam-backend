# 프로젝트 지원 — 락 소유 시간 / 락 경합 부하 테스트

## 1. 뭘 측정하는 건가?

프로젝트 지원(`apply()`)은 트랜잭션 **초반**에 아래 UPDATE를 실행한다.

```sql
UPDATE project SET application_count = application_count + 1 WHERE project_id = ?
```

InnoDB는 이 UPDATE가 잡은 project 행 **X-lock을 커밋 시점까지** 쥔다.
즉, 같은 프로젝트에 동시에 지원이 몰리면 **두 번째 지원자부터는 앞 트랜잭션이
커밋될 때까지 줄을 서야 한다.** 락을 쥐는 구간은:

```
UPDATE (락 획득) → 지원서 INSERT → 답변 저장(EAV) → 알림 저장 → 커밋 (락 해제)
```

이 테스트는 두 가지를 측정한다.

| 측정 대상 | 도구 |
|---|---|
| **락 소유 시간** (UPDATE 후 ~ 커밋까지, 요청 1건이 락을 쥔 시간) | 서버에 심은 Micrometer 타이머 |
| **락 경합의 결과** (동시 요청이 몰릴 때 응답 지연·실패) | k6 부하 테스트 |

서버(`ProjectApplicationServiceImpl`)에 심어둔 메트릭:

- `project.apply.lock.held` — 락 획득 직후 → 커밋 완료까지 (= 락 소유 시간)
- `project.apply.lock.acquire` — UPDATE 문 자체 수행 시간. **다른 트랜잭션의 락을
  기다린 시간이 여기에 포함**되므로, 경합이 심할수록 이 값이 커진다.
- 요청마다 서버 로그에도 찍힌다: `project 행 락 소유 시간 - projectId: ..., held: ...ms`

> **[구조 변경] 현재 브랜치는 카운터 증가를 비동기로 분리했다.**
> 위 설명은 1단계(트랜잭션 안 동기 UPDATE) 기준이고, 지금 코드는 커밋 후
> `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 리스너(`ApplicationCountEventListener`)의
> **별도 트랜잭션**에서 UPDATE 한다.
>
> - apply 트랜잭션: 지원서 INSERT의 FK 검증이 잡는 project 행 **S-lock만** 쥔다
>   → S-lock끼리는 공존하므로 동시 지원이 더 이상 직렬화되지 않는다
> - 카운터 트랜잭션: X-lock 소유가 **UPDATE → 커밋**으로 최소화.
>   `lock.held` / `lock.acquire` 는 이 리스너 안에서 측정된다
> - 트레이드오프: 카운터는 최종 일관성(수 ms 지연 반영), 커밋 직후 서버가 죽으면
>   해당 건 카운트 유실 가능 (통계성 값이라 허용)
> - 전용 executor(`applicationCountExecutor`, 스레드 2 + 큐 20,000)가 버스트를 흡수하고,
>   큐가 넘치면 CallerRunsPolicy로 호출 스레드가 직접 실행해 유실을 막는다
> - `apply-one.sh` 는 비동기 반영을 최대 3초 폴링해서 "이번 1건" 값을 보여준다

### 1-1. 왜 이 측정값이 "락 소유 시간"인가 (측정 원리)

측정 코드는 두 시점의 `System.nanoTime()` 차이를 기록한다.

```java
projectRepository.increaseApplicationCount(projectId); // UPDATE 실행
// ① UPDATE가 리턴된 직후 — 시작점
...
// ② afterCompletion 콜백 — Spring이 JDBC commit()이 완료된 직후 호출 — 끝점
```

이 구간이 곧 락 소유 시간인 근거는 InnoDB의 **락 수명**이 구조적으로 정해져 있기 때문이다.

- **시작점에 락을 확실히 쥐고 있다** — InnoDB는 UPDATE 문을 실행하는 도중에
  행 X-lock을 획득한다. 따라서 UPDATE가 리턴된 시점(①)엔 이미 락 소유 중이다.
- **끝점에 락이 확실히 풀려 있다** — InnoDB는 커밋 처리 중에 행 락을 해제한다.
  `afterCompletion`(②)은 JDBC `commit()` 호출이 리턴된 뒤에 불리므로 그 시점엔 락이 해제됐다.

즉 측정 구간 [①→②]는 실제 락 소유 구간을 빈틈없이 덮는 **상한값**이며,
오차는 커밋 응답이 드라이버로 돌아오는 수십 µs 수준이다(로컬호스트에선 더 작음).

**교차 검증이 필요하면** — DB 쪽 타임스탬프로 직접 확인할 수 있다:

```sql
SET GLOBAL general_log = 'ON';
SET GLOBAL log_output = 'TABLE';
-- ./loadtest/apply-one.sh 302 실행 후, 같은 thread_id의 UPDATE ↔ commit 시각 차이 확인
SELECT event_time, thread_id, command_type, LEFT(argument, 80) AS query
FROM mysql.general_log
WHERE argument LIKE '%application_count%' OR LOWER(argument) = 'commit'
ORDER BY event_time DESC LIMIT 20;
SET GLOBAL general_log = 'OFF';
```

"락이 커밋까지 유지된다" 자체를 눈으로 보려면 터미널 2개로:

```sql
-- 터미널 A: 커밋하지 않고 멈춰 있기
BEGIN;
UPDATE project SET application_count = application_count + 1 WHERE project_id = 302;

-- 터미널 B: 같은 UPDATE 실행 → A가 COMMIT 할 때까지 블로킹되는 것을 확인
UPDATE project SET application_count = application_count + 1 WHERE project_id = 302;
```

## 2. 사전 준비 (최초 1회)

```bash
# 1) 시드: 지원자 회원 20,000명 + 핫 프로젝트 생성
#    마지막 SELECT가 hot_project_id를 출력한다 → 현재 302
mysql -u root -h 127.0.0.1 -P 3306 meeteam < loadtest/seed.sql

# 2) JWT 토큰 20,000개 사전 발급 → loadtest/tokens.json
#    (로그인이 세종대 포털에 의존하므로, 서버와 같은 시크릿으로 미리 찍어둔다)
./gradlew generateLoadTestTokens -Pcount=20000
```

## 3. 서버 기동

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

> 측정 코드는 서버에 들어있으므로, **코드 수정 후에는 반드시 재시작**해야 메트릭이 잡힌다.

## 4. 단건 측정 — 경합 없을 때 락을 얼마나 쥐나 (기준값)

부하 없이 지원 1건만 보내서 "요청 하나가 락을 쥐는 순수 시간"을 잰다.
부하 테스트 결과와 비교할 기준값이 된다.

**4-1. 웜업 (서버 재시작 후 필수)**

첫 요청은 콜드 스타트(JIT 컴파일, Hibernate 쿼리 플랜 캐시, 커넥션 준비) 비용이 얹혀서
100ms 이상 나오는 게 정상이다. 같은 요청을 반복해서 보내는 것이 곧 웜업이다.

```bash
for i in {1..20}; do ./loadtest/apply-one.sh 302; done
```

실행마다 `이번 1건 락 소유 시간: X.XXms`가 출력되는데, 처음 몇 건은 크게 나오다가
대략 10회쯤부터 수렴한다.

**4-2. 측정**

웜업 루프가 끝난 뒤 한 번 더 실행하고, 그 `이번 1건` 값을 기준값으로 기록한다.

```bash
./loadtest/apply-one.sh 302          # 토큰 자동 소비 (실행할 때마다 다음 회원으로 지원)
./loadtest/apply-one.sh 302 19999    # 토큰 인덱스 직접 지정
```

- 출력의 `누적 평균/최대`에는 콜드 값이 섞여 있으므로 무시하고 **`이번 1건` 값**을 쓴다.
- **토큰 충돌 없음**: k6는 토큰을 0번부터 올라가며 쓰고, 이 스크립트는 마지막(19999번)부터
  내려가며 쓴다. 그래서 웜업/단건 측정으로 지원한 회원이 부하 테스트에서
  "이미 지원함" 에러를 내지 않는다. (합계가 토큰 수 20,000을 넘지 않는 한 안전 —
  가장 큰 burst도 ~17,800건이라 여유 있음)

## 5. 부하 테스트 — 동시 지원이 몰리면 어떻게 되나

같은 프로젝트(302)에 초당 수십~수백 건의 지원을 투입한다.
`ramping-arrival-rate`(open model)라서 **서버가 느려져도 k6는 투입 속도를 유지한다**
— 락 대기로 응답이 밀리는 현상을 숨기지 않기 위함이다.

```bash
# baseline: 5→100 req/s로 3분간 점진 증가, 약 6,500건 — 경합 발생 직전까지
k6 run -e PROJECT_ID=302 loadtest/apply-load.js

# peak: 50→250 req/s, 100초, 약 15,700건 — 처리 상한 돌파
k6 run -e PROJECT_ID=302 -e PROFILE=peak loadtest/apply-load.js

# burst: 100→1000 req/s, 약 30초, 약 17,800건 — 전면 붕괴 관찰용 (에러 자체가 측정 대상)
k6 run -e PROJECT_ID=302 -e PROFILE=burst loadtest/apply-load.js
```

회원당 1회만 지원할 수 있으므로 토큰을 1회성으로 소비한다 → **재실행 전 reset 필수**(6번).

## 6. 재실행 전 초기화

핫 프로젝트의 지원서/답변/알림을 지우고 카운터를 0으로 되돌린다. (회원/토큰은 재사용)

```bash
mysql -u root -h 127.0.0.1 -P 3306 meeteam < loadtest/reset.sql
```

## 7. 결과 확인

```bash
# 락 소유 시간 (서버 기동 후 누적: COUNT / TOTAL_TIME / MAX)
curl localhost:8080/actuator/metrics/project.apply.lock.held

# UPDATE 수행 시간 (락 대기 포함 — 경합 지표)
curl localhost:8080/actuator/metrics/project.apply.lock.acquire
```

- 평균 = `TOTAL_TIME ÷ COUNT`, 단위는 초(s).
- 메트릭은 **첫 지원 요청이 들어와야 생긴다.** 404면 아직 0건이거나 서버가 구 빌드.

테스트 중 MySQL에서 락 경합을 직접 보려면:

```sql
-- 지금 누가 누구를 기다리는지 (실시간)
SELECT r.trx_mysql_thread_id AS waiting_thread, r.trx_query AS waiting_query,
       b.trx_mysql_thread_id AS blocking_thread, b.trx_query AS blocking_query
FROM performance_schema.data_lock_waits w
JOIN information_schema.innodb_trx r ON r.trx_id = w.REQUESTING_ENGINE_TRANSACTION_ID
JOIN information_schema.innodb_trx b ON b.trx_id = w.BLOCKING_ENGINE_TRANSACTION_ID;

-- 누적 지표 (테스트 전후 비교: waits 횟수, 평균/최대 대기 시간)
SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';
```

## 8. 기록할 지표 (2단계 슬롯 분산과 비교용)

| 지표 | 출처 |
|---|---|
| 락 소유 시간 평균/최대 | `project.apply.lock.held` |
| 락 대기 포함 UPDATE 시간 | `project.apply.lock.acquire` |
| p95 / p99 응답 시간 | k6 `http_req_duration` |
| 실제 처리율 vs 목표 투입율 | k6 `dropped_iterations`, `http_reqs` |
| 행 락 대기 횟수 / 평균 대기 시간 | `Innodb_row_lock_waits`, `Innodb_row_lock_time_avg` |
| 락 타임아웃/에러율 | k6 `http_req_failed` + 서버 로그 |

## 구성 요소

- `seed.sql` — 지원자 회원 N명 + 리더 + 핫 프로젝트(302) + 모집 상태. 재실행 안전(증분 삽입).
- `reset.sql` — 핫 프로젝트의 지원서/답변/알림 삭제 + 카운터 0 초기화.
- `apply-load.js` — k6 램핑 시나리오 (baseline / peak / burst).
- `apply-one.sh` — 단건 지원 + 락 소유 시간 즉시 확인.
- `tokens.json` — `./gradlew generateLoadTestTokens`가 생성 (git 추적 제외).