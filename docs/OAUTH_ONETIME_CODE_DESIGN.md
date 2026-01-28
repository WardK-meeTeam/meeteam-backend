# OAuth 일회용 코드 방식 설계 문서

## 1. 개요

### 현재 문제점
- OAuth 로그인 성공 후 리다이렉트 시 **JWT 토큰이 URL 쿼리 파라미터에 노출**됨
- 브라우저 히스토리, 서버 로그, Referer 헤더를 통해 토큰 유출 가능
- 기존 회원의 경우 **실제 Access Token**이 URL에 그대로 노출

### 개선 방향
- URL에는 **짧은 일회용 코드(UUID)** 만 노출
- 실제 토큰은 **API 응답 body**로만 전달
- Redis를 활용하여 일회용 코드의 **1회 사용 보장** 및 **TTL 자동 만료**

---

## 2. 전체 플로우

### 2-1. 신규 회원 (최초 소셜 로그인 → 회원가입)

```
사용자 브라우저                    백엔드 서버                     소셜 서버 (Google/GitHub)
      │                              │                                    │
      │  [1] "구글로 로그인" 클릭      │                                    │
      │─────────────────────────────>│                                    │
      │                              │                                    │
      │  [2] 302 Redirect            │                                    │
      │<─────────────────────────────│                                    │
      │                              │                                    │
      │  [3] 구글 로그인 & 동의       │                                    │
      │──────────────────────────────────────────────────────────────────>│
      │                              │                                    │
      │                              │  [4] 인가코드 전달                   │
      │                              │<───────────────────────────────────│
      │                              │                                    │
      │                              │  [5] 인가코드로 사용자 정보 요청      │
      │                              │───────────────────────────────────>│
      │                              │                                    │
      │                              │  [6] email, name, providerId 응답   │
      │                              │<───────────────────────────────────│
      │                              │                                    │
      │                              │  [7] DB 조회 → 신규 회원            │
      │                              │  [8] Redis 저장:                    │
      │                              │      Key: oauth:code:{UUID}        │
      │                              │      Value: {email, provider,      │
      │                              │              providerId, type}     │
      │                              │      TTL: 10분 (회원가입 폼 작성 시간 고려) │
      │                              │                                    │
      │  [9] 302 Redirect            │                                    │
      │  → /oauth2/redirect          │                                    │
      │    ?code={UUID}              │                                    │
      │    &type=register            │                                    │
      │<─────────────────────────────│                                    │
      │                              │                                    │
      │  [10] 회원가입 폼 표시         │                                    │
      │  사용자가 추가 정보 입력       │                                    │
      │  (이름, 나이, 성별, 기술스택)   │                                    │
      │                              │                                    │
      │  [11] POST /api/auth/register/oauth2                              │
      │  Body: {code, name, age,     │                                    │
      │         gender, skills, ...}  │                                    │
      │─────────────────────────────>│                                    │
      │                              │                                    │
      │                              │  [12] Redis에서 code 조회           │
      │                              │  → 소셜 정보 꺼냄                   │
      │                              │  → Redis에서 즉시 삭제              │
      │                              │  → Member 생성 + DB 저장            │
      │                              │  → Access/Refresh 토큰 발급         │
      │                              │                                    │
      │  [13] 200 OK                 │                                    │
      │  Body: {accessToken, name}   │                                    │
      │  Cookie: refreshToken        │                                    │
      │<─────────────────────────────│                                    │
      │                              │                                    │
      │  로그인 완료                   │                                    │
```

### 2-2. 기존 회원 (재로그인)

```
사용자 브라우저                    백엔드 서버                     소셜 서버
      │                              │                                │
      │  [1]~[6] 동일 (OAuth 인증)    │                                │
      │                              │                                │
      │                              │  [7] DB 조회 → 기존 회원        │
      │                              │  [8] Redis 저장:                │
      │                              │      Key: oauth:code:{UUID}    │
      │                              │      Value: {memberId, type}   │
      │                              │      TTL: 60초                 │
      │                              │                                │
      │  [9] 302 Redirect            │                                │
      │  → /oauth2/redirect          │                                │
      │    ?code={UUID}              │                                │
      │    &type=login               │                                │
      │<─────────────────────────────│                                │
      │                              │                                │
      │  [10] POST /api/auth/token/exchange                           │
      │  Body: {code}                │                                │
      │─────────────────────────────>│                                │
      │                              │                                │
      │                              │  [11] Redis에서 code 조회       │
      │                              │  → memberId 꺼냄               │
      │                              │  → Redis에서 즉시 삭제          │
      │                              │  → DB에서 Member 조회           │
      │                              │  → Access/Refresh 토큰 발급     │
      │                              │                                │
      │  [12] 200 OK                 │                                │
      │  Body: {accessToken, name}   │                                │
      │  Cookie: refreshToken        │                                │
      │<─────────────────────────────│                                │
      │                              │                                │
      │  로그인 완료                   │                                │
```

---

## 3. Redis 저장 구조

### 신규 회원

```
Key:   "oauth:code:a3f8b2c1-xxxx-xxxx-xxxx"
Value: {
    "email": "user@gmail.com",
    "provider": "google",
    "providerId": "1234567890",
    "oauthAccessToken": "ya29.xxx",   // OAuth 토큰 철회용
    "type": "register"
}
TTL:   10분 (회원가입 폼 작성 시간 고려)
```

### 기존 회원

```
Key:   "oauth:code:x7d2e9f4-xxxx-xxxx-xxxx"
Value: {
    "memberId": 42,
    "oauthAccessToken": "ya29.xxx",   // OAuth 토큰 철회용
    "type": "login"
}
TTL:   60초
```

---

## 4. API 엔드포인트 변경 사항

### 4-1. 기존 유지

| 메서드 | 경로 | 용도 | 변경 |
|--------|------|------|------|
| POST | `/api/auth/register` | 일반 회원가입 | 변경 없음 |
| POST | `/api/auth/login` | 일반 로그인 | 변경 없음 |
| POST | `/api/auth/refresh` | 토큰 갱신 | 변경 없음 |
| POST | `/api/auth/logout` | 로그아웃 | 변경 없음 |

### 4-2. 변경

| 메서드 | 경로 | 변경 내용 |
|--------|------|-----------|
| POST | `/api/auth/register/oauth2` | Request에서 `token` → `code`로 변경 |

### 4-3. 신규

| 메서드 | 경로 | 용도 |
|--------|------|------|
| POST | `/api/auth/token/exchange` | 기존 OAuth 회원의 일회용 코드 → 토큰 교환 |

---

## 5. 현재 vs 개선 비교

| 항목 | 현재 방식 | 개선 후 |
|------|-----------|---------|
| URL에 노출되는 것 | JWT 토큰 (긴 문자열) | UUID 코드 (36자) |
| 코드/토큰 재사용 | TTL 내 가능 | 불가 (1회 사용 후 삭제) |
| 실제 토큰 전달 방식 | URL 쿼리 파라미터 | API 응답 body |
| 서버 즉시 무효화 | 불가 (JWT 특성) | 가능 (Redis 삭제) |
| 추가 인프라 | 없음 | Redis (이미 사용 중) |
| 신규/기존 구분 | 토큰 종류가 다름 | 동일 패턴, `type`만 다름 |
| 프론트 처리 | URL에서 토큰 추출 후 바로 사용 | URL에서 code 추출 → API 호출 |

---

## 6. 보안 고려사항

- **TTL**: 기존 회원 60초 / 신규 회원 10분 (회원가입 폼 작성 시간 고려)
- **일회성**: Redis에서 조회 즉시 삭제하여 재사용 불가
- **타입 검증**: `type` 필드로 register/login 용도 구분, 교차 사용 방지
- **HTTPS 필수**: 리다이렉트 URL은 반드시 HTTPS 사용
