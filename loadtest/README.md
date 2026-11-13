# 프로젝트 지원 락 경합 부하 테스트

`apply()` 트랜잭션 초반의 `UPDATE project SET application_count = application_count + 1`이
project 행 X-lock을 커밋까지 쥐면서 생기는 경합을 측정한다. (1단계: 단일 카운터 기준선)

## 실행 순서

```bash
# 1. 시드 (회원 20,000명 + 핫 프로젝트) — 마지막 SELECT가 hot_project_id를 출력한다
#    증분 삽입이라 seed.sql의 @member_count를 늘려 재실행하면 부족분만 추가된다
mysql -u <user> -p <database> < loadtest/seed.sql

# 2. 토큰 사전 발급 → loadtest/tokens.json (시드 회원 수와 맞출 것)
./gradlew generateLoadTestTokens -Pcount=20000

# 3. 서버 기동 (local 프로필)
./gradlew bootRun

# 4. 부하 실행
k6 run -e PROJECT_ID=<hot_project_id> loadtest/apply-load.js                 # baseline: 5→100 req/s, 3분, ~6.5천 건
k6 run -e PROJECT_ID=<hot_project_id> -e PROFILE=peak loadtest/apply-load.js # peak: 50→250 req/s, 100초, ~15.7천 건

# 재실행 전: 지원서 초기화 (회원/토큰은 재사용)
mysql -u <user> -p <database> < loadtest/reset.sql
```

## 락 경합 관찰 (테스트 중 MySQL에서)

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

## 기록할 지표 (2단계 슬롯 분산과 비교용)

| 지표 | 출처 |
|---|---|
| p95 / p99 응답 시간 | k6 `http_req_duration` |
| 실제 처리율 vs 목표 투입율 | k6 `dropped_iterations`, `http_reqs` |
| 행 락 대기 횟수 / 평균 대기 시간 | `Innodb_row_lock_waits`, `Innodb_row_lock_time_avg` |
| 락 타임아웃/에러율 | k6 `http_req_failed` + 서버 로그 |

## 구성 요소

- `seed.sql` — 지원자 회원 N명 + 리더 + 핫 프로젝트 + 모집 상태. 재실행 안전.
- `reset.sql` — 핫 프로젝트의 지원서/답변/알림 삭제 + 카운터 0으로 초기화.
- `apply-load.js` — k6 램핑 시나리오. 회원당 1회 지원 제약 때문에 토큰을
  `iterationInTest`로 1회성 소비한다.
- `tokens.json` — `./gradlew generateLoadTestTokens`가 생성 (git 추적 제외).
  로그인이 세종대 포털(외부)에 의존하므로, 서버와 동일한 시크릿으로 토큰을
  사전 발급하는 토큰 팩토리 방식을 쓴다 (`LoadTestTokenGenerator`).