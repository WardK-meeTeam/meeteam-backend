# 회원가입 API 흐름

## 개요

회원가입 시 사용자는 **직군(JobField) → 직무(JobPosition) → 기술스택(TechStack)** 순서로 선택합니다.
- JobField/JobPosition은 **ENUM 코드**로 식별 (타입 안전성, 환경 독립성)
- TechStack은 **ID**로 식별 (종류가 많아 동적 관리 필요)
- TechStack 선택 시 **displayOrder**로 사용자가 선택한 순서를 저장

## 회원가입 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                        프론트엔드                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 1. GET /api/jobs/options (인증 불필요)                           │
│    - 전체 직군/직무/기술스택 옵션 조회                              │
│    - 프론트에서 선택 UI 구성에 사용                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. 사용자 입력                                                   │
│    - 기본 정보: 이메일, 비밀번호, 이름, 생년월일, 성별               │
│    - 관심분야: JobField → JobPosition 선택 (중복 가능)            │
│    - 기술스택: 선택한 JobPosition별로 TechStack 선택 (순서 저장)    │
│    - 프로젝트 경험 횟수, GitHub URL, 블로그 URL                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. POST /api/auth/register (일반 회원가입)                       │
│    또는                                                          │
│    POST /api/auth/register/oauth2 (OAuth2 회원가입)              │
└─────────────────────────────────────────────────────────────────┘
```

## API 상세

### 1. 직군/직무/기술스택 옵션 조회

```http
GET /api/jobs/options
```

**인증:** 불필요 (화이트리스트)

**Response:**
```json
{
  "code": "_OK",
  "message": "OK",
  "data": {
    "fields": [
      {
        "code": "BACKEND",
        "name": "백엔드",
        "positions": [
          {
            "code": "JAVA_SPRING",
            "name": "Java/Spring"
          },
          {
            "code": "KOTLIN_SPRING",
            "name": "Kotlin/Spring"
          }
        ],
        "techStacks": [
          {
            "id": 1,
            "name": "Java"
          },
          {
            "id": 2,
            "name": "Spring Boot"
          }
        ]
      }
    ]
  }
}
```

### 2. 일반 회원가입

```http
POST /api/auth/register
Content-Type: multipart/form-data
```

**Request:**
- `request` (JSON): 회원가입 정보
- `file` (optional): 프로필 이미지

**Request Body (request 파트):**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "birthDate": "1998-03-15",
  "gender": "MALE",
  "jobPositions": [
    {
      "jobFieldCode": "BACKEND",
      "jobPositionCode": "JAVA_SPRING",
      "techStacks": [
        { "id": 1, "displayOrder": 1 },
        { "id": 2, "displayOrder": 2 },
        { "id": 3, "displayOrder": 3 }
      ]
    },
    {
      "jobFieldCode": "FRONTEND",
      "jobPositionCode": "WEB_FRONTEND",
      "techStacks": [
        { "id": 10, "displayOrder": 1 },
        { "id": 11, "displayOrder": 2 }
      ]
    }
  ],
  "projectExperienceCount": 3,
  "githubUrl": "https://github.com/username",
  "blogUrl": "https://blog.example.com"
}
```

**Response:**
```json
{
  "code": "_OK",
  "message": "OK",
  "data": {
    "username": "홍길동",
    "memberId": 1
  }
}
```

### 3. OAuth2 회원가입

```http
POST /api/auth/register/oauth2
Content-Type: multipart/form-data
```

**Request:**
- `request` (JSON): 회원가입 정보
- `file` (optional): 프로필 이미지

**Request Body (request 파트):**
```json
{
  "code": "550e8400-e29b-41d4-a716-446655440000",
  "name": "홍길동",
  "birthDate": "2000-01-01",
  "gender": "MALE",
  "jobPositions": [
    {
      "jobFieldCode": "BACKEND",
      "jobPositionCode": "JAVA_SPRING",
      "techStacks": [
        { "id": 1, "displayOrder": 1 },
        { "id": 2, "displayOrder": 2 },
        { "id": 3, "displayOrder": 3 }
      ]
    }
  ],
  "projectExperienceCount": 3,
  "githubUrl": "https://github.com/username",
  "blogUrl": "https://blog.example.com"
}
```

**Response:**
```json
{
  "code": "_OK",
  "message": "OK",
  "data": {
    "username": "홍길동",
    "memberId": 1,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

## ENUM 코드 목록

### JobFieldCode (직군)

| 코드 | 설명 |
|------|------|
| `PLANNING` | 기획 |
| `DESIGN` | 디자인 |
| `FRONTEND` | 프론트 |
| `BACKEND` | 백엔드 |
| `AI` | AI |
| `INFRA_OPERATION` | 인프라/운영 |

### JobPositionCode (직무)

| 코드 | 직군 | 설명 |
|------|------|------|
| `PRODUCT_MANAGER` | PLANNING | PM 프로덕트 매니저 |
| `PRODUCT_OWNER` | PLANNING | PO 프로덕트 오너 |
| `SERVICE_PLANNER` | PLANNING | 서비스 기획 |
| `UI_UX_DESIGNER` | DESIGN | UI/UX 디자이너 |
| `MOTION_DESIGNER` | DESIGN | 모션 디자이너 |
| `BX_BRAND_DESIGNER` | DESIGN | BX 브랜드 디자이너 |
| `WEB_FRONTEND` | FRONTEND | 웹 프론트엔드 |
| `IOS` | FRONTEND | iOS |
| `ANDROID` | FRONTEND | Android |
| `CROSS_PLATFORM` | FRONTEND | 크로스 플랫폼 |
| `JAVA_SPRING` | BACKEND | Java/Spring |
| `KOTLIN_SPRING` | BACKEND | Kotlin/Spring |
| `NODE_NESTJS` | BACKEND | Node.js/NestJS |
| `PYTHON_BACKEND` | BACKEND | Python Backend |
| `MACHINE_LEARNING` | AI | 머신 러닝 |
| `DEEP_LEARNING` | AI | 딥러닝 |
| `LLM` | AI | LLM |
| `MLOPS` | AI | MLOps |
| `DEVOPS_ARCHITECT` | INFRA_OPERATION | DevOps 엔지니어/아키텍처 |
| `QA` | INFRA_OPERATION | QA |
| `CLOUD_ENGINEER` | INFRA_OPERATION | Cloud 엔지니어 |

## 검증 규칙

1. **직무-직군 매칭**: 선택한 `jobPositionCode`는 해당 `jobFieldCode`에 속해야 함
2. **기술스택-직군 매칭**: 선택한 `techStacks`의 ID는 해당 `jobFieldCode`에 속하는 기술스택이어야 함
3. **관심분야 필수**: 최소 1개 이상의 직무 선택 필수
4. **비밀번호**: 최소 8자 이상
5. **displayOrder**: 1 이상의 정수, 기술스택 표시 순서를 나타냄

## 에러 코드

| 코드 | 설명 |
|------|------|
| `JOB_FIELD_NOT_FOUND` | 존재하지 않는 직군 코드 |
| `JOB_POSITION_NOT_FOUND` | 존재하지 않는 직무 코드 |
| `IS_NOT_ALLOWED_POSITION` | 직무가 해당 직군에 속하지 않음 |
| `TECH_STACK_NOT_FOUND` | 존재하지 않는 기술스택 ID |
| `TECH_STACK_IS_NOT_MATCHING` | 기술스택이 해당 직군에 속하지 않음 |
| `DUPLICATE_MEMBER` | 이미 존재하는 이메일 |
| `INVALID_PASSWORD_PATTERN` | 비밀번호 형식 오류 |
