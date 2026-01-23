# 프론트엔드 인증 가이드

## 수정된 버그 및 개선 사항 (2026-01-23)

### 1. 로그아웃 API 엔드포인트 수정

**문제:** 로그아웃이 어떨 때는 되고 어떨 때는 안 되는 현상

**원인:** 엔드포인트 경로 오타
- 잘못된 경로: `POST /api/auth/api/auth/logout`
- 올바른 경로: `POST /api/auth/logout`

**수정 완료:** 이제 `POST /api/auth/logout`으로 요청하면 정상 동작합니다.

### 2. AccessToken 블랙리스트 구현 (NEW)

**개선:** 로그아웃 시 AccessToken이 Redis 블랙리스트에 등록되어, 해당 토큰으로 더 이상 API 호출이 불가능합니다.

**중요 변경사항:** 로그아웃 요청 시 반드시 `Authorization` 헤더에 AccessToken을 포함해야 합니다!

---

## 현재 인증 플로우

### 1. 일반 로그인
```
POST /login
Body: { "email": "...", "password": "..." }

Response:
- Header: Authorization: Bearer {accessToken}
- Cookie: refreshToken (HttpOnly, Secure)
```

### 2. OAuth2 로그인 (Google/GitHub)
```
1. GET /oauth2/authorization/google (또는 github)
   → OAuth2 제공자로 리다이렉트

2. 콜백 후 리다이렉트:
   - 기존 회원: /?token={accessToken}
   - 신규 회원: /oauth2/signup?token={signupToken}
```

### 3. 토큰 재발급
```
POST /api/auth/refresh
Header: Authorization: Bearer {만료된accessToken} (선택)
Cookie: refreshToken (필수)

Response:
- Body: { "result": "{newAccessToken}" }
```

### 4. 로그아웃 (변경됨!)
```
POST /api/auth/logout
Header: Authorization: Bearer {accessToken}  ← 필수! (블랙리스트 등록용)

Response:
- Set-Cookie: refreshToken 삭제 (maxAge=0)
- Body: { "result": "로그아웃이 완료되었습니다." }
```

**주의:** Authorization 헤더를 포함해야 해당 토큰이 블랙리스트에 등록됩니다.

---

## 프론트엔드 필수 구현 사항

### 1. AccessToken 관리

**저장 위치:** 메모리 또는 sessionStorage (localStorage 비권장)

```javascript
// 저장
sessionStorage.setItem('accessToken', token);

// API 요청 시
fetch('/api/...', {
  headers: {
    'Authorization': `Bearer ${sessionStorage.getItem('accessToken')}`
  }
});
```

### 2. 로그아웃 시 AccessToken 전송 및 삭제 (중요!)

```javascript
async function logout() {
  const accessToken = sessionStorage.getItem('accessToken');

  try {
    await fetch('/api/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`  // 필수! 블랙리스트 등록용
      },
      credentials: 'include'  // 쿠키 포함 필수
    });
  } finally {
    // 서버 응답과 관계없이 반드시 삭제
    sessionStorage.removeItem('accessToken');
    // 또는 메모리에서 삭제
  }
}
```

**중요 변경사항:**
- 로그아웃 시 `Authorization` 헤더에 AccessToken을 포함해야 블랙리스트에 등록됩니다
- 서버에서 RefreshToken 쿠키 삭제 + AccessToken 블랙리스트 등록
- **프론트엔드에서도 AccessToken을 반드시 삭제**해야 합니다

### 3. 토큰 만료 처리

```javascript
// API 응답 인터셉터
async function apiRequest(url, options) {
  let response = await fetch(url, options);

  if (response.status === 401) {
    // 토큰 재발급 시도
    const refreshResponse = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include'
    });

    if (refreshResponse.ok) {
      const data = await refreshResponse.json();
      sessionStorage.setItem('accessToken', data.result);

      // 원래 요청 재시도
      options.headers['Authorization'] = `Bearer ${data.result}`;
      response = await fetch(url, options);
    } else {
      // 재발급 실패 → 로그인 페이지로 이동
      sessionStorage.removeItem('accessToken');
      window.location.href = '/login';
    }
  }

  return response;
}
```

### 4. OAuth2 콜백 처리

```javascript
// OAuth2 콜백 페이지 (예: /oauth2/redirect)
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');

if (token) {
  // URL에서 토큰 제거 (보안)
  window.history.replaceState({}, document.title, window.location.pathname);

  // 토큰 저장
  sessionStorage.setItem('accessToken', token);

  // 메인 페이지로 이동
  window.location.href = '/';
}
```

---

## 알려진 이슈 및 권장사항

### 1. AccessToken 블랙리스트 구현 완료 (해결됨)

**현재 상태:** 로그아웃 시 AccessToken이 Redis 블랙리스트에 등록되어 즉시 무효화됩니다.

**프론트엔드 필수 대응:**
- 로그아웃 요청 시 반드시 `Authorization` 헤더에 AccessToken 포함
- 로그아웃 후 프론트엔드에서도 AccessToken 삭제
- 다른 탭에서도 로그아웃 상태 동기화 권장

```javascript
// 다른 탭 로그아웃 감지
window.addEventListener('storage', (e) => {
  if (e.key === 'accessToken' && e.newValue === null) {
    window.location.href = '/login';
  }
});
```

### 2. RefreshToken Rotation 미구현

**현재 상태:** 토큰 재발급 시 RefreshToken이 갱신되지 않습니다.

**리스크:** RefreshToken 탈취 시 7일간 악용 가능

**프론트엔드 대응:** 특별한 조치 불필요 (백엔드 개선 사항)

### 3. OAuth2 토큰 URL 노출

**현재 상태:** OAuth2 로그인 후 AccessToken이 URL 쿼리 파라미터로 전달됩니다.

**프론트엔드 대응:**
- 토큰 수신 후 즉시 `history.replaceState()`로 URL에서 제거
- 브라우저 히스토리에 토큰이 남지 않도록 처리

---

## API 에러 코드

| 에러 코드 | HTTP Status | 설명 | 프론트엔드 대응 |
|-----------|-------------|------|----------------|
| `AUTH001` | 401 | 인증 실패 | 로그인 페이지로 이동 |
| `AUTH002` | 401 | AccessToken 만료 | 토큰 재발급 시도 |
| `AUTH003` | 401 | RefreshToken 없음 | 로그인 페이지로 이동 |
| `AUTH004` | 401 | RefreshToken 만료 | 로그인 페이지로 이동 |
| `AUTH005` | 401 | 유효하지 않은 RefreshToken | 로그인 페이지로 이동 |

---

## 토큰 정보

| 토큰 | 만료 시간 | 저장 위치 |
|------|----------|----------|
| AccessToken | 10시간 | 프론트엔드 (메모리/sessionStorage) |
| RefreshToken | 7일 | HttpOnly Cookie |

---

## 체크리스트

- [ ] 로그아웃 시 AccessToken 삭제 구현
- [ ] 401 응답 시 토큰 재발급 로직 구현
- [ ] OAuth2 콜백에서 URL 토큰 제거 구현
- [ ] API 요청에 `credentials: 'include'` 설정 (쿠키 전송용)
- [ ] CORS 설정으로 인한 쿠키 문제 확인

---

## 문의

백엔드 관련 문의사항은 Slack #backend 채널로 연락 부탁드립니다.
