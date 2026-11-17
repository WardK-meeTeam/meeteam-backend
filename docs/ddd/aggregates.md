# Aggregate 경계 정의 (초안)

> 목적: MeeTeam 도메인의 **어그리게이트 경계를 확정**하고, 그 근거를 남긴다.
> 이 문서는 Week 2 Mission 2-3의 산출물이며, 코드 리팩터링의 **설계도** 역할을 한다.
> ⚠️ 초안(draft). 각 결정의 `상태` 칸을 채우며 확정해 나간다.

판단 기준은 단 하나 (`aggregate-쉽게이해하기.md` 참고):
> **"이 둘은 같은 순간(같은 트랜잭션)에 반드시 함께 맞아야 하는 규칙(불변식)이 있는가?"**
> YES → 같은 Aggregate / NO → 다른 Aggregate(ID 참조 + 결과적 일관성)

---

## 1. 어그리게이트 지도 (목표 상태)

```
   ★ = Aggregate Root        ─── 객체 참조(같은 Aggregate 내)   ┄┄ ID 참조(Aggregate 간)

   ┌─ Aggregate A: PROJECT ───────────────────┐
   │  ★ Project                                │
   │     ├─ ProjectMember        (합류 멤버)    │
   │     ├─ RecruitmentState     (직군별 모집)  │
   │     │     └─ RecruitmentTechStack          │
   │     └─ ProjectSkill         (기술스택)     │
   │   불변식: currentCount ≤ recruitmentCount  │
   └────────────────────────────────────────────┘
        ┊projectId        ┊projectId         ┊memberId
   ┌────┴─────────────┐ ┌─┴──────────────┐ ┌─┴────────────┐
   │ ★ ProjectApplic. │ │ ★ ProjectLike  │ │ ★ Member     │
   │   (지원서)        │ │  (좋아요)       │ │  (회원)       │
   └──────────────────┘ └────────────────┘ └──────────────┘
```

---

## 2. 어그리게이트별 정의

### Aggregate A. Project (핵심 어그리게이트)

| 항목 | 내용 |
|---|---|
| **Root** | `Project` |
| **구성원** | `ProjectMember`, `RecruitmentState`(→`RecruitmentTechStack`), `ProjectSkill` |
| **핵심 불변식** | 직군별 합류 멤버 수(`currentCount`) ≤ 모집 정원(`recruitmentCount`); 모든 직군이 차면 모집 상태 CLOSED |
| **묶는 근거** | `addMember()`에서 멤버 합류 + `increaseCurrentCount()` + `updateRecruitmentsStatus()`가 **한 트랜잭션에서 함께** 맞아야 한다. 중간 상태가 노출되면 정원 초과 버그. |
| **외부 참조 방식** | 멤버는 `Member`를 ID로 참조(`memberId`) 권장 |
| **상태** | ☐ 확정 / ☑ 검토중 |

> 참고: `ProjectMember`는 자기 생명주기가 약하고(프로젝트에 종속) 정원 불변식에 직접 엮이므로 Project 내부 구성원으로 두는 게 자연스럽다. `RecruitmentState`가 전용 Repository(`RecruitmentStateRepository`)를 갖고 있는데, 이게 경계 위반인지 점검 필요 → §4.

---

### Aggregate B. ProjectApplication (지원서) — ⚠️ 현재 "양다리" 상태

| 항목 | 내용 |
|---|---|
| **Root** | `ProjectApplication` (독립 루트로 확정 권장) |
| **구성원** | 없음 (단일 엔티티) |
| **자기 불변식** | 상태 전이: PENDING → ACCEPTED / REJECTED / CANCELLED (역방향·중복 결정 금지) |
| **분리 근거** | 지원 시점에 프로젝트 정원이 즉시 바뀌지 않음(합류는 *수락* 시). 지원서는 수십~수백 개 누적. 자기만의 생명주기 보유. → 같은 순간 함께 맞아야 할 규칙 **없음**. |
| **상태** | ☐ 확정 / ☑ 검토중 |

#### 🔴 현재 코드의 모순 (반드시 해소)

| 관점 | 현재 | 문제 |
|---|---|---|
| JPA 매핑 | `Project.applications` `@OneToMany(cascade=ALL, orphanRemoval=true)` | Project 어그리게이트의 **내부 구성원**처럼 묶임 |
| 역참조 | `ProjectApplication.project` **객체 참조** | 어그리게이트 간 객체 참조 → 경계 흐려짐 |
| 접근 | `ProjectApplicationRepository.findById()` 로 직접 조회 | **독립 루트**처럼 사용 |
| Project 내부 로직 | `getPendingApplicationCount()`, `rejectAllPendingApplications()` | Project가 지원서 컬렉션을 직접 순회 |

→ **구조는 묶였는데 사용은 독립.** 둘 중 "독립 어그리게이트"로 통일해야 함.

#### 정리 방향 (목표)
1. `Project.applications` cascade 컬렉션 **제거** (Project가 지원서를 자식으로 들지 않음).
2. `ProjectApplication.project` → `Long projectId`, `applicant` → `Long applicantId` (ID 참조).
3. cross-aggregate 작업 전환:
   - `rejectAllPendingApplications()` (프로젝트 삭제 시) → **도메인 이벤트** `ProjectDeleted` 발행 후 지원서 쪽이 구독 처리, 또는 Application Service가 두 Repository 조율.
   - `getPendingApplicationCount()` → 조회는 `ProjectApplicationRepository`의 count 쿼리로.
4. 수락 흐름: `application.accept()` + `project.acceptMember(...)` 를 Application Service가 조율 (이미 우리가 합의한 그림).

⚠️ cascade 삭제 동작과 기존 쿼리에 영향 → **특성화 테스트로 보호 후 단계적** 적용.

---

### Aggregate C. ProjectLike (좋아요)

| 항목 | 내용 |
|---|---|
| **Root** | `ProjectLike` |
| **분리 근거** | 좋아요는 프로젝트 정원/멤버 규칙과 무관. 즉시 일관성 불필요. |
| **특이점** | `Project.likeCount`는 **비정규화 캐시**. 진짜 좋아요(`ProjectLike`)와의 정합성은 **결과적 일관성**으로 충분(`@Retry` 낙관적 락으로 카운트 보정 중). |
| **상태** | ☐ 확정 / ☑ 검토중 |

---

### Aggregate D. Member (회원)

| 항목 | 내용 |
|---|---|
| **Root** | `Member` |
| **분리 근거** | 독립적 생명주기(가입/탈퇴/프로필). 다른 어그리게이트는 `memberId`로 참조. |
| **상태** | ☐ 확정 / ☑ 검토중 |

---

## 3. 어그리게이트 간 참조 규칙 (목표)

| From | To | 현재 | 목표 |
|---|---|---|---|
| ProjectApplication | Project | 객체 참조 | `Long projectId` |
| ProjectApplication | Member(applicant) | 객체 참조 | `Long applicantId` |
| ProjectMember | Member | 객체 참조 | `Long memberId` (검토) |
| ProjectLike | Project / Member | 객체 참조 | ID 참조 |

> 원칙: **다른 어그리게이트는 객체가 아니라 ID로 참조.** 필요 시 해당 Repository로 명시적 재조회.
> (지연 로딩으로 남의 어그리게이트 그래프를 타고 들어가지 않는다 → N+1·LazyInit 예방)

---

## 4. 미해결 질문 (결정 필요)

- [ ] **`RecruitmentStateRepository`**: `RecruitmentState`가 Project 어그리게이트의 내부 구성원이라면, 전용 Repository로 직접 조회/저장하는 게 경계 위반 아닌가? (조회 전용으로만 허용할지, 제거할지)
- [ ] **`ProjectMember`의 Member 참조**: ID 참조로 바꾸면 `getProjectMembers` 응답 조립이 번거로워진다. 읽기는 Projection(CQRS)으로 우회 가능한지 검토.
- [ ] **cascade 제거 시 삭제 정책**: 프로젝트 삭제 시 지원서/좋아요 처리를 이벤트로 할지, 명시적 서비스 조율로 할지.
- [ ] **트랜잭션 경계**: 수락 시 두 어그리게이트(Application, Project)를 같은 트랜잭션에서 바꿀지(현재처럼), 이벤트로 분리할지.

---

## 5. 적용 우선순위 (Week 2 → 이후)

1. **(문서)** 본 경계 확정 — 위 `상태` 칸을 ☑확정으로.
2. **(테스트)** 지원/수락/삭제 흐름 특성화 테스트 그린 확보.
3. **(코드 1차)** `ProjectApplication` 상태 전이 메서드 정리(`accept/reject/cancel`) — 어그리게이트 내부 정돈.
4. **(코드 2차)** `Project.applications` cascade 제거 + ID 참조 전환 — 경계 분리.
5. **(코드 3차)** cross-aggregate 작업을 이벤트/서비스 조율로 이전.

> 각 단계는 테스트 그린 유지하며 진행. 한 번에 다 하지 않는다.
