# 시스템 분석 보고서 (SYSTEM_ANALYSIS_REPORT)

## 1. API 엔드포인트 목록 및 기능 요약

본 시스템은 도메인별로 컨트롤러가 분리되어 있으며, 주요 엔드포인트는 다음과 같습니다.

### 1.1 인증 (Authentication) - `/api/auth`
- **회원가입**: 일반 이메일 가입 및 OAuth2(Google, GitHub)를 통한 간편 가입을 지원합니다.
- **로그인/갱신**: JWT(Json Web Token) 기반 인증을 사용하며, Access Token 만료 시 Refresh Token을 이용한 갱신 메커니즘을 제공합니다.
- **로그아웃**: 사용자의 Refresh Token을 무효화하여 로그아웃 처리합니다.

### 1.2 프로젝트 관리 (Project Management) - `/api/projects`
- **CRUD**: 프로젝트의 생성, 상세 조회(V2), 수정, 삭제 기능을 제공합니다.
- **상세 검색**: 카테고리, 기술 스택, 모집 분야 등 다양한 필터를 조합하여 프로젝트를 검색할 수 있습니다.
- **상태 관리**: 진행 중인 프로젝트를 '완료(Completed)' 또는 '종료(Ended)' 상태로 변경합니다.

### 1.3 프로젝트 멤버 및 지원 (Members & Applications) - `/api/projects/{projectId}`
- **지원 프로세스**: 사용자가 프로젝트에 지원서를 제출하고(`POST /application`), 팀장은 지원 목록을 조회하여 승인/거절합니다.
- **멤버 관리**: 프로젝트 참여 멤버 목록을 조회하고, 팀장은 멤버를 내보내거나 멤버 스스로 탈퇴할 수 있습니다.

### 1.4 회원 프로필 (Member Profile) - `/api/members`
- **프로필 관리**: 사용자의 기본 정보, 기술 스택, 포트폴리오 URL 등을 조회하고 수정합니다.
- **멤버 검색**: 닉네임 등을 통해 다른 사용자를 검색할 수 있습니다.

### 1.5 알림 (Notifications) - `/api/notifications`
- **실시간 구독**: SSE(Server-Sent Events)를 통해 실시간 알림 스트림을 구독합니다.
- **알림 관리**: 읽지 않은 알림 목록을 조회하고 읽음 처리합니다.

---

## 2. 핵심 비즈니스 규칙

프로젝트 전반, 특히 Service 계층(`ProjectServiceImpl`, `ProjectApplicationServiceImpl` 등)에 정의된 핵심 비즈니스 규칙은 다음과 같습니다.

### 2.1 프로젝트 생성 및 관리 규칙
- **날짜 유효성 검증**:
    - 프로젝트 종료일은 반드시 시작일 이후여야 합니다.
    - 종료일은 현재 날짜(오늘)보다 이전일 수 없습니다. (`INVALID_PROJECT_DATE`)
- **상태에 따른 제약**:
    - **완료된 프로젝트(Completed)**: 더 이상 수정하거나 삭제할 수 없으며, 신규 지원도 불가능합니다. (`PROJECT_ALREADY_COMPLETED`)
    - **리뷰 작성**: 오직 '완료된' 프로젝트에 대해서만 상호 리뷰 작성이 가능합니다. (`PROJECT_NOT_COMPLETED`)
- **권한 제어**:
    - 프로젝트 수정, 삭제, 멤버 관리(승인/거절/강퇴)는 오직 프로젝트 생성자(팀장)만 수행할 수 있습니다. (`PROJECT_MEMBER_FORBIDDEN`)
    - 팀장은 프로젝트에서 탈퇴하거나 제거될 수 없습니다. (`CREATOR_DELETE_FORBIDDEN`)

### 2.2 모집 및 지원 규칙
- **중복 지원 방지**:
    - 사용자는 동일한 프로젝트에 중복해서 지원할 수 없습니다. (`PROJECT_APPLICATION_ALREADY_EXISTS`)
    - 이미 프로젝트의 멤버인 사용자는 다시 지원할 수 없습니다. (`PROJECT_MEMBER_ALREADY_EXISTS`)
- **모집 인원 제한**:
    - 각 모집 분야(SubCategory)별로 정해진 인원이 가득 차면 더 이상 지원을 승인할 수 없습니다. (`RECRUITMENT_FULL`)

### 2.3 회원 및 인증 규칙
- **이메일 유일성**: 시스템 내에서 이메일은 중복될 수 없습니다. (`DUPLICATE_MEMBER`)
- **OAuth2 필수 정보**: 소셜 로그인 시 제공자(Provider)로부터 이메일과 Provider ID를 반드시 획득해야 합니다.

### 2.4 파일 업로드 정책
- **유효성 검사**: 업로드 파일의 확장자는 허용된 목록(이미지 등)에 포함되어야 하며(`INVALID_FILE_EXTENSION`), 설정된 최대 용량을 초과할 수 없습니다(`FILE_SIZE_EXCEEDED`).

---

## 3. 잠재적 버그 및 코드 스멜

### 3.1 성능상 심각한 N+1 문제 (Critical N+1 Query Problem)
- **위치**: `ProjectServiceImpl.searchProject` 및 `searchMainPageProject`
- **문제점**:
    - 프로젝트 목록을 조회하는 메서드(`findAllSliced...`) 실행 후, 결과 리스트(`content`)를 순회(`map`)하면서 내부에서 추가적인 DB 조회를 수행합니다.
    - `projectCategoryApplicationRepository.findTotalCountsByProject(project)` (모집 현황 조회)
    - `projectMemberServiceImpl.getProjectMembers(project.getId())` (멤버 목록 조회)
    - `projectLikeRepository.existsByMemberIdAndProjectId(...)` (좋아요 여부 조회)
- **위험성**: 페이징 사이즈가 20개일 경우, 한 번의 API 호출에 대해 **1(목록 조회) + 20 × 3(추가 조회) = 총 61번의 쿼리**가 발생합니다. 사용자가 늘어나고 데이터가 쌓일수록 시스템 성능이 급격히 저하될 수 있는 치명적인 병목 지점입니다.

### 3.2 동시성 처리의 취약점 (Concurrency Vulnerability)
- **위치**: `ProjectMemberServiceImpl.addMember`
- **문제점**: 모집 인원을 증가시키는 로직(`increaseCurrentCount`)에 `@Retry` 어노테이션이 적용되어 있습니다. 이는 `ObjectOptimisticLockingFailureException`과 같은 충돌 발생 시 재시도를 수행한다는 의미입니다.
- **위험성**: 단순 재시도(Retry)는 경합이 적을 때는 유효하지만, 인기 있는 프로젝트에 수십/수백 명의 지원자가 동시에 몰릴 경우 재시도 횟수를 소진하고 요청이 실패할 수 있습니다. 또한 DB 부하를 가중시킬 수 있습니다.

### 3.3 환경 설정의 하드코딩 (Hardcoded Configuration)
- **위치**: `AuthController` 및 OAuth 관련 클래스
- **문제점**: 쿠키 생성 로직 등에 도메인 주소(예: `.meeteam.alom-sejong.com`)가 문자열 리터럴로 직접 박혀있습니다.
- **위험성**: 개발(Local), 검증(Staging), 운영(Production) 환경별로 도메인이 다를 텐데, 이를 코드로 관리하면 배포 시마다 소스 코드를 수정해야 하는 불편함과 실수가 발생할 수 있습니다. (`application.yml`로 분리 필요)

### 3.4 관리되지 않는 코드 (Dead/Commented Code)
- **위치**: `ChatController.java`
- **문제점**: 클래스 파일 전체가 주석 처리되어 방치되어 있습니다.
- **위험성**: 해당 기능이 개발 중인지, 폐기된 것인지 파악하기 어려워 유지보수 혼란을 야기하며 코드 가독성을 해칩니다.

### 3.5 예외 처리의 모호함 (Generic Exception Handling)
- **위치**: 다수의 컨트롤러 및 서비스
- **문제점**: 일부 로직에서 구체적인 예외 대신 `CustomException`이나 `RuntimeException`을 포괄적으로 사용하거나, 에러 메시지가 클라이언트에게 구체적인 원인을 전달하지 못하는 경우가 있습니다.
- **위험성**: 프론트엔드 개발자가 API 에러의 원인(입력값 오류 vs 서버 오류 vs 권한 없음)을 명확히 구분하여 사용자에게 안내하기 어렵습니다.
