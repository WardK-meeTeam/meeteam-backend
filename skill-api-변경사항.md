# 기술 스택(Skill) API 변경사항

## 변경 요약

기술 스택 관련 필드가 API마다 제각각이었던 것을 **전부 통일**했습니다.

### 변경 원칙

- **필드명**: 전부 `skills`로 통일
- **타입**: 전부 `string[]` (문자열 배열)로 통일
- 요청/응답 모두 동일한 형태

---

## Before / After 비교

### 1. 회원가입 (POST /api/auth/register)

```diff
// Request Body
{
  "name": "홍길동",
  "age": 27,
  "email": "test@test.com",
  "password": "qwer1234",
  "gender": "MALE",
  "subCategories": [...],
- "skills": [{ "skillName": "Java" }, { "skillName": "Spring" }]
+ "skills": ["Java", "Spring"]
}
```

### 2. OAuth2 회원가입 (POST /api/auth/oauth2/register)

```diff
// Request Body
{
  "code": "550e8400-...",
  "name": "홍길동",
  "age": 27,
  "gender": "MALE",
  "subCategories": [...],
- "skills": [{ "skillName": "Java" }, { "skillName": "Spring" }]
+ "skills": ["Java", "Spring"]
}
```

### 3. 프로필 수정 (PUT /api/members)

```
// Request Body - 변경 없음 (기존에도 문자열 배열)
{
  "skills": ["Java", "Spring", "MySQL"]
}
```

### 4. 프로필 조회 (GET /api/members, GET /api/members/{id})

```diff
// Response
{
  "name": "홍길동",
  "memberId": 1,
- "skills": [{ "skill": "Java" }, { "skill": "Spring" }]
+ "skills": ["Java", "Spring"]
}
```

### 5. 멤버 카드 조회 (GET /api/members/all)

```diff
// Response
{
  "memberId": 1,
  "realName": "홍길동",
- "skillList": ["Java", "Spring"]
+ "skills": ["Java", "Spring"]
}
```

### 6. 멤버 검색 (GET /api/members/search)

```diff
// Query Parameter
- GET /api/members/search?skillList=Java&skillList=Spring
+ GET /api/members/search?skills=Java&skills=Spring
```

### 7. 프로젝트 생성 (POST /api/projects)

```diff
// Request Body
{
  "projectName": "test",
  "description": "test description",
  "projectCategory": "ENVIRONMENT",
  "platformCategory": "IOS",
  "offlineRequired": true,
  "subCategory": "웹프론트엔드",
  "recruitments": [...],
- "projectSkills": [{ "skillName": "Java" }, { "skillName": "Spring" }],
+ "skills": ["Java", "Spring"],
  "endDate": "2025-12-31"
}
```

### 8. 프로젝트 수정 (PUT /api/projects/{id})

```diff
// Request Body
{
  "name": "test",
  "recruitments": [...],
- "skills": [{ "skillName": "Java" }, { "skillName": "Spring" }]
+ "skills": ["Java", "Spring"]
}
```

### 9. 프로젝트 검색 (GET /api/projects/search)

```diff
// Response
{
  "projectId": 1,
  "projectCategory": "ENVIRONMENT",
  "platformCategory": "IOS",
- "projectSkills": [{ "name": "Java" }, { "name": "Spring" }],
+ "skills": ["Java", "Spring"],
  "projectName": "test",
  "creatorName": "홍길동",
  "localDate": "2025-01-01"
}
```

### 10. 프로젝트 조건 검색 (GET /api/projects/condition)

```diff
// Response
{
  "projectId": 1,
- "projectSkills": ["Java", "Spring"],
+ "skills": ["Java", "Spring"],
  "projectName": "test",
  ...
}
```

### 11. 메인 페이지 프로젝트 (GET /api/mainpage/projects)

```diff
// Response
{
  "projectId": 1,
- "projectSkills": ["Java", "Spring"],
+ "skills": ["Java", "Spring"],
  "projectName": "test",
  ...
}
```

### 12. 메인 페이지 프로젝트 카드

```diff
// Response
{
  "platformCategory": "IOS",
- "projectSkills": [{ "name": "Java" }, { "name": "Spring" }],
+ "skills": ["Java", "Spring"],
  "projectName": "test",
  ...
}
```

### 13. 프로젝트 목록 조회

```
// Response - 변경 없음 (기존에도 문자열 배열 + skills 필드명)
{
  "skills": ["Java", "Spring"]
}
```

---

## 변경 요약 표

| API | 변경 전 필드명 | 변경 전 타입 | 변경 후 필드명 | 변경 후 타입 |
|---|---|---|---|---|
| 회원가입 (요청) | `skills` | `{ skillName: string }[]` | `skills` | `string[]` |
| OAuth2 회원가입 (요청) | `skills` | `{ skillName: string }[]` | `skills` | `string[]` |
| 프로필 수정 (요청) | `skills` | `string[]` | `skills` | `string[]` (변경 없음) |
| 프로필 조회 (응답) | `skills` | `{ skill: string }[]` | `skills` | `string[]` |
| 멤버 카드 (응답) | `skillList` | `string[]` | `skills` | `string[]` |
| 멤버 검색 (쿼리 파라미터) | `skillList` | `string[]` | `skills` | `string[]` |
| 프로젝트 생성 (요청) | `projectSkills` | `{ skillName: string }[]` | `skills` | `string[]` |
| 프로젝트 수정 (요청) | `skills` | `{ skillName: string }[]` | `skills` | `string[]` |
| 프로젝트 검색 (응답) | `projectSkills` | `{ name: string }[]` | `skills` | `string[]` |
| 프로젝트 조건 검색 (응답) | `projectSkills` | `string[]` | `skills` | `string[]` |
| 메인 페이지 프로젝트 (응답) | `projectSkills` | `string[]` | `skills` | `string[]` |
| 메인 페이지 카드 (응답) | `projectSkills` | `{ name: string }[]` | `skills` | `string[]` |
| 프로젝트 목록 (응답) | `skills` | `string[]` | `skills` | `string[]` (변경 없음) |

---

## 프론트 작업 체크리스트

- [ ] 회원가입 폼: `skills` 필드를 `["Java", "Spring"]` 형태로 전송
- [ ] OAuth2 회원가입 폼: 동일하게 수정
- [ ] 프로필 조회: `response.skills`가 문자열 배열로 바뀜 (`.skill` 접근 제거)
- [ ] 멤버 카드: `skillList` -> `skills`로 필드명 변경
- [ ] 멤버 검색: 쿼리 파라미터 `skillList` -> `skills`로 변경
- [ ] 프로젝트 생성: `projectSkills` -> `skills`, 객체 배열 -> 문자열 배열
- [ ] 프로젝트 수정: 객체 배열 -> 문자열 배열
- [ ] 프로젝트 검색 결과: `projectSkills` -> `skills`, 객체 배열 -> 문자열 배열
- [ ] 프로젝트 조건 검색 결과: `projectSkills` -> `skills`
- [ ] 메인 페이지 프로젝트: `projectSkills` -> `skills`
- [ ] 메인 페이지 카드: `projectSkills` -> `skills`, 객체 배열 -> 문자열 배열
