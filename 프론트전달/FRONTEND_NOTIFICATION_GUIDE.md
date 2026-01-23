# 프론트엔드 알림 시스템 가이드

## 개요

MeeTeam의 알림 시스템은 **SSE (Server-Sent Events)** 기반의 실시간 단방향 통신을 사용합니다.

- **SSE**: 알림 (서버 → 클라이언트 단방향)
- **WebSocket/STOMP**: 채팅 (양방향, 별도 구현)

---

## SSE 연결

### 구독 엔드포인트

```
GET /api/subscribe
```

### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Authorization` | O | Bearer {accessToken} |
| `Last-Event-ID` | X | 마지막 수신 이벤트 ID (재연결 시 사용) |

### 응답

- **Content-Type**: `text/event-stream`
- **타임아웃**: 1시간 (무활동 시 자동 종료)

### SSE 이벤트 형식

```
event: {알림타입}
id: {memberId}_{timestamp}
data: {JSON 페이로드}
```

### 클라이언트 구현 예시

```javascript
// SSE 연결 시작
function connectSSE() {
  const accessToken = sessionStorage.getItem('accessToken');
  const lastEventId = localStorage.getItem('lastEventId') || '';

  // EventSource는 커스텀 헤더를 지원하지 않으므로 fetch 또는 라이브러리 사용
  // 방법 1: URL 쿼리 파라미터 사용 (백엔드 지원 시)
  // 방법 2: EventSource polyfill 라이브러리 사용

  const eventSource = new EventSource('/api/subscribe');

  // 연결 확인 (PING)
  eventSource.addEventListener('PING', (e) => {
    console.log('SSE 연결 성공');
  });

  // 알림 타입별 이벤트 리스너
  eventSource.addEventListener('PROJECT_APPLY', handleProjectApply);
  eventSource.addEventListener('PROJECT_MY_APPLY', handleProjectMyApply);
  eventSource.addEventListener('PROJECT_APPROVE', handleProjectApprove);
  eventSource.addEventListener('PROJECT_REJECT', handleProjectReject);
  eventSource.addEventListener('PROJECT_END', handleProjectEnd);

  // 에러 처리 및 재연결
  eventSource.addEventListener('error', (e) => {
    if (e.target.readyState === EventSource.CLOSED) {
      console.log('SSE 연결 종료, 재연결 시도...');
      setTimeout(connectSSE, 3000); // 3초 후 재연결
    }
  });

  return eventSource;
}

// 이벤트 핸들러 예시
function handleProjectApply(event) {
  const notification = JSON.parse(event.data);
  console.log('지원 알림:', notification);

  // 마지막 이벤트 ID 저장 (재연결 시 사용)
  localStorage.setItem('lastEventId', event.lastEventId);

  // UI 업데이트
  showNotificationBadge();
  showToast(`${notification.data.applicantName}님이 '${notification.data.projectName}'에 지원했습니다`);
}
```

### 인증 포함 SSE 연결 (권장)

EventSource는 커스텀 헤더를 지원하지 않으므로, `eventsource` 폴리필 또는 fetch 기반 구현을 권장합니다.

```javascript
// fetch 기반 SSE 구현
async function connectSSEWithAuth() {
  const accessToken = sessionStorage.getItem('accessToken');
  const lastEventId = localStorage.getItem('lastEventId') || '';

  const response = await fetch('/api/subscribe', {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Last-Event-ID': lastEventId
    }
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const text = decoder.decode(value);
    parseSSEMessage(text);
  }
}
```

---

## 알림 API

### 1. 알림 목록 조회

```
GET /api/notifications
```

**쿼리 파라미터:**

| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| `page` | 0 | 페이지 번호 |
| `size` | 20 | 페이지 크기 |
| `sort` | createdAt,desc | 정렬 |

**요청 예시:**
```javascript
const response = await fetch('/api/notifications?page=0&size=20', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

**응답:**
```json
{
  "code": "COMMON200",
  "result": {
    "content": [
      {
        "id": 1,
        "type": "PROJECT_APPLY",
        "message": null,
        "isRead": true,
        "createdAt": "2024-01-15",
        "applicationId": 5,
        "payload": {
          "applicationId": 5,
          "projectId": 10,
          "receiverId": 1,
          "applicantId": 2,
          "applicantName": "김철수",
          "projectName": "AI 채팅봇 개발",
          "date": "2024-01-15"
        }
      }
    ],
    "hasNext": false,
    "hasContent": true
  }
}
```

**주의:** 이 API 호출 시 **모든 알림이 자동으로 읽음 처리**됩니다.

### 2. 읽지 않은 알림 개수 조회

```
GET /api/notifications/unread/count
```

**응답:**
```json
{
  "code": "COMMON200",
  "result": {
    "unReadCount": 3
  }
}
```

---

## 알림 타입

| 타입 | 설명 | 수신자 |
|------|------|--------|
| `PROJECT_APPLY` | 프로젝트 지원 알림 | 프로젝트 팀장 |
| `PROJECT_MY_APPLY` | 지원 완료 알림 | 지원자 본인 |
| `PROJECT_APPROVE` | 지원 승인 알림 | 지원자 |
| `PROJECT_REJECT` | 지원 거절 알림 | 지원자 |
| `PROJECT_END` | 프로젝트 종료 알림 | 프로젝트 팀원 전체 |

---

## 타입별 페이로드 구조

### PROJECT_APPLY (지원 알림)

```json
{
  "type": "PROJECT_APPLY",
  "data": {
    "applicationId": 5,
    "projectId": 10,
    "receiverId": 1,
    "applicantId": 2,
    "applicantName": "김철수",
    "projectName": "AI 채팅봇 개발",
    "date": "2024-01-15"
  },
  "createdAt": "2024-01-15"
}
```

**UI 예시:** "김철수님이 'AI 채팅봇 개발'에 지원했습니다"

### PROJECT_MY_APPLY (지원 완료 알림)

```json
{
  "type": "PROJECT_MY_APPLY",
  "data": {
    "receiverId": 2,
    "projectName": "AI 채팅봇 개발",
    "localDate": "2024-01-15"
  },
  "createdAt": "2024-01-15"
}
```

**UI 예시:** "'AI 채팅봇 개발'에 지원이 완료되었습니다"

### PROJECT_APPROVE (승인 알림)

```json
{
  "type": "PROJECT_APPROVE",
  "data": {
    "receiverId": 2,
    "projectId": 10,
    "approvalResult": "APPROVED",
    "date": "2024-01-15"
  },
  "createdAt": "2024-01-15"
}
```

**UI 예시:** "프로젝트 지원이 승인되었습니다"

### PROJECT_REJECT (거절 알림)

```json
{
  "type": "PROJECT_REJECT",
  "data": {
    "receiverId": 2,
    "projectId": 10,
    "approvalResult": "REJECTED",
    "date": "2024-01-15"
  },
  "createdAt": "2024-01-15"
}
```

**UI 예시:** "프로젝트 지원이 거절되었습니다"

### PROJECT_END (프로젝트 종료 알림)

```json
{
  "type": "PROJECT_END",
  "data": {
    "projectId": 10,
    "memberId": 2,
    "projectName": "AI 채팅봇 개발",
    "occurredAt": "2024-01-15"
  },
  "createdAt": "2024-01-15"
}
```

**UI 예시:** "'AI 채팅봇 개발' 프로젝트가 종료되었습니다"

---

## 재연결 메커니즘

### 이벤트 ID 형식

```
{memberId}_{timestamp}
예: 123_1705123456789
```

### 재연결 시 미수신 이벤트 복구

1. 클라이언트가 `Last-Event-ID` 헤더에 마지막 이벤트 ID 전송
2. 서버가 해당 timestamp 이후의 모든 캐시된 이벤트 자동 재전송
3. 최대 7일간 이벤트 캐시 보관

```javascript
// 재연결 시 Last-Event-ID 전달
const lastEventId = localStorage.getItem('lastEventId');

fetch('/api/subscribe', {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Last-Event-ID': lastEventId || ''
  }
});
```

---

## 다중 탭/기기 지원

- 같은 사용자가 여러 탭/기기에서 접속 시, 모든 활성 SSE 연결에 알림 브로드캐스트
- 각 탭마다 별도의 SSE 연결 유지
- 이벤트 ID는 `{memberId}_{timestamp}` 형식으로 사용자별 고유 식별

---

## 구현 체크리스트

### SSE 연결

- [ ] `/api/subscribe` 연결 구현
- [ ] `Last-Event-ID` 헤더 전송 (재연결 시)
- [ ] `PING` 이벤트로 연결 확인
- [ ] 에러 발생 시 자동 재연결 로직
- [ ] 타임아웃 (1시간) 후 재연결

### 이벤트 핸들러

- [ ] `PROJECT_APPLY` 핸들러
- [ ] `PROJECT_MY_APPLY` 핸들러
- [ ] `PROJECT_APPROVE` 핸들러
- [ ] `PROJECT_REJECT` 핸들러
- [ ] `PROJECT_END` 핸들러

### 상태 관리

- [ ] 마지막 이벤트 ID 저장 (localStorage)
- [ ] 읽지 않은 알림 개수 표시
- [ ] 알림 목록 페이징 (무한 스크롤)

### UI/UX

- [ ] 알림 뱃지 (읽지 않은 개수)
- [ ] 실시간 토스트 알림
- [ ] 알림 목록 페이지
- [ ] 알림 클릭 시 관련 페이지로 이동

---

## 주의사항

1. **SSE 타임아웃**: 1시간 무활동 시 자동 종료 → 재연결 필요
2. **이벤트 캐시 보관**: 최대 7일 (이후 자동 삭제)
3. **자동 읽음 처리**: `/api/notifications` 조회 시 모든 알림이 읽음으로 변경
4. **인증 필요**: 모든 알림 API는 AccessToken 필요
5. **다중 탭**: 여러 탭에서 동시 접속 가능, 모든 탭에 알림 전송

---

## 문의

백엔드 관련 문의사항은 Slack #backend 채널로 연락 부탁드립니다.
