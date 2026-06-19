# Aggregate, 내 프로젝트 코드로 이해하기

> "어그리게이트가 뭔지 감이 안 온다"에 대한 답.
> 이론 말고 **네 엔티티(Project, RecruitmentState, ProjectMember, ProjectApplication, ProjectLike)** 로만 설명한다.

---

## 1. 한 문장으로

> **Aggregate = "항상 함께 일관성을 지켜야 하는 객체 묶음" 하나를, 대표(Root) 한 명을 통해서만 다루는 것.**

비유부터. 회사의 **주문서**를 생각해라.
- 주문(Order) 안에 주문항목(OrderLine)들이 있다.
- "주문 총액 = 항목 합계"라는 규칙은 **항상, 즉시** 맞아야 한다.
- 그래서 너는 OrderLine을 **따로** 저장소에서 꺼내 바꾸지 않는다. 항상 **Order를 통해** 바꾼다.
- → `Order`가 **Aggregate Root**, `OrderLine`은 그 **내부 구성원**, 둘이 합쳐 하나의 **Aggregate**.

핵심은 "포함 관계"가 아니라 **"같이 맞아야 하는 규칙(불변식)이 있느냐"** 다.

---

## 2. 어그리게이트를 가르는 단 하나의 질문

복잡하게 생각하지 마라. 두 객체를 두고 이것만 물어라:

> ### "이 둘은 **같은 순간(같은 트랜잭션)** 에 반드시 함께 맞아야 하는 규칙이 있는가?"
>
> - **YES** → 같은 Aggregate. (한 묶음으로 저장/수정)
> - **NO** → 다른 Aggregate. (각자 저장, ID로만 참조, 약간 늦게 맞아도 됨)

이 질문을 네 코드에 그대로 던져보자.

---

## 3. 네 프로젝트에 적용 — 진짜 경계 긋기

### 후보 엔티티들
`Project`, `RecruitmentState`(직군별 모집현황), `ProjectMember`(합류한 멤버), `ProjectApplication`(지원서), `ProjectLike`(좋아요), `Member`

### 🔬 하나씩 질문 던지기

#### Q. Project ↔ RecruitmentState ↔ ProjectMember — 같은 순간에 맞아야 할 규칙이 있나?

있다. 그것도 아주 강하게. 네 `addMember` 코드를 보자:

```java
project.joinMember(projectMember);          // 멤버 합류
recruitmentState.increaseCurrentCount();    // 그 직군 정원 +1
project.updateRecruitmentsStatus();         // 다 찼으면 모집 마감 처리
```

여기엔 **즉시 지켜져야 하는 불변식**이 숨어 있다:

> 🔒 **"직군별 합류 멤버 수(`currentCount`)는 모집 정원(`recruitmentCount`)을 초과할 수 없다."**

만약 멤버 합류는 됐는데 정원 카운트가 안 올라간 **중간 상태**가 잠깐이라도 보이면? → 정원 초과 버그.
이 셋은 **한 트랜잭션에서 반드시 함께** 맞아야 한다. → **같은 Aggregate.**

```
┌─────────────────────── Aggregate ①: Project ───────────────────────┐
│                                                                     │
│   ★ Project  (Aggregate Root = 유일한 출입구)                        │
│      ├── List<ProjectMember>      (합류 멤버)                        │
│      ├── List<RecruitmentState>   (직군별 모집 현황)                  │
│      │       └── List<RecruitmentTechStack>                         │
│      └── List<ProjectSkill>       (프로젝트 기술스택)                 │
│                                                                     │
│   불변식: Σ currentCount ≤ Σ recruitmentCount, 다 차면 모집 CLOSED   │
└─────────────────────────────────────────────────────────────────────┘
```

➡️ 그래서 바깥에서 `RecruitmentState`를 **직접** 꺼내 `increaseCurrentCount()` 하면 안 된다.
반드시 `project.acceptMember(...)` 처럼 **Root(Project)를 통해** 바꿔야 정원 규칙을 Project가 지킬 수 있다.
(지금 `addMember` 서비스가 `recruitmentState`를 직접 만지는 게 바로 이 경계를 살짝 넘는 지점이다.)

---

#### Q. Project ↔ ProjectApplication(지원서) — 같은 순간에 맞아야 할 규칙이 있나?

생각해보자. 누군가 **지원**할 때, 프로젝트의 멤버 수나 정원이 그 즉시 바뀌나? → **아니다.** 지원은 그냥 "지원함"이고, 합류는 나중에 **수락**해야 일어난다.

- 지원서는 **수십, 수백 개**가 쌓일 수 있다. 프로젝트 규칙 하나 지키자고 지원서 전부를 메모리에 올릴 이유가 없다.
- 지원서는 **자기만의 생명주기**가 있다: PENDING → ACCEPTED / REJECTED / CANCELLED.

→ "같은 순간에 함께 맞아야 할 규칙"이 **없다.** → **다른 Aggregate!**

```
┌──── Aggregate ②: ProjectApplication ────┐
│   ★ ProjectApplication (자기가 Root)      │
│      status: PENDING→ACCEPTED/...         │
│      projectId  ← Project를 ID로만 참조    │  (객체 통째로 X)
│      applicantId ← Member를 ID로만 참조    │
└───────────────────────────────────────────┘
```

➡️ 그래서 "지원이 자기 안에서 멤버까지 추가"하면 안 된다고 한 거다. 지원서(Aggregate②)가 프로젝트(Aggregate①)의 내부(멤버 목록)를 직접 바꾸는 거니까. **남의 집 냉장고를 직접 여는 셈.**

➡️ 수락 흐름은 그래서 **Application Service가 두 Aggregate를 차례로 조율**한다:
```java
application.accept();                    // Aggregate② 자기 상태만 변경
project.acceptMember(applicantId, jobPos); // Aggregate① 자기 멤버/정원만 변경
```

---

#### Q. Project ↔ ProjectLike(좋아요) — 같은 순간에 맞아야 하나?

좋아요 1개 누를 때 프로젝트의 정원/멤버 규칙이 함께 깨질 일이 있나? → 없다.
좋아요는 그냥 독립적으로 쌓인다. → **또 다른 Aggregate.**

😈 여기 재밌는 함정: `Project.likeCount` 필드 + `increaseLike()` + `@Retry`(낙관적 락)가 있다.
`likeCount`는 사실 **"좋아요 개수를 미리 세어둔 캐시(비정규화)"** 다. 진짜 좋아요들(`ProjectLike`)은 별도 Aggregate인데, 그 개수를 Project가 들고 있는 것.

> 이게 바로 **"Aggregate 간 일관성은 '즉시'가 아니라 '결과적으로' 맞추면 된다(eventual consistency)"** 의 실전 사례다.
> 좋아요가 1초 늦게 카운트에 반영돼도 세상 안 망한다. 정원 초과는 즉시 막아야 하지만.

---

## 4. 그래서 네 프로젝트의 Aggregate 지도

```
   ★ = Aggregate Root (출입구)        ─── 객체 참조 (같은 Aggregate 안)
                                       ┄┄┄ ID 참조 (Aggregate 사이, 느슨)

   ┌─ Aggregate: PROJECT ─────────────┐
   │  ★ Project                       │
   │     ├─ ProjectMember             │
   │     ├─ RecruitmentState ─ RecruitmentTechStack
   │     └─ ProjectSkill              │
   └──────────────────────────────────┘
          ┊                ┊
          ┊ projectId      ┊ projectId
          ┊                ┊
   ┌──────┴──────────┐  ┌──┴────────────────┐   ┌─────────────────┐
   │ ★ ProjectApplic.│  │ ★ ProjectLike     │   │ ★ Member        │
   │   applicantId ┄┄┼┄┄┼┄ memberId ┄┄┄┄┄┄┄┄┼┄┄┄┤  (독립 Root)    │
   └─────────────────┘  └───────────────────┘   └─────────────────┘
```

- **Project Aggregate**: 멤버·모집현황·기술스택. "정원 초과 금지" 불변식이 묶어준다. → 한 묶음.
- **ProjectApplication / ProjectLike / Member**: 각자 독립 Root. Project를 **ID로만** 참조.

---

## 5. 지금 네 코드가 어긋난 지점 (왜 이게 미션인지)

지금 `Project`는 **모든 걸 cascade로 다 들고 있다**:
```java
@OneToMany(... cascade = ALL) List<ProjectMember> members;
@OneToMany(... cascade = ALL) List<ProjectApplication> applications;   // ← 사실 다른 Aggregate인데!
@OneToMany(... cascade = ALL) List<ProjectLike> projectLikes;          // ← 이것도!
@OneToMany(... cascade = ALL) List<ProjectSkill> projectSkills;
@OneToMany(... cascade = ALL) List<RecruitmentState> recruitments;
```

이건 **"전부 하나의 거대한 Aggregate"** 상태다. 가장 흔한 초기 실수. 부작용:
- 좋아요 하나 누르려고 프로젝트 + 지원서 전부 + 멤버 전부를 로드.
- `application.getProject().getCreator().getEmail()` 처럼 남의 Aggregate 속을 줄줄이 헤집는 코드(디미터 법칙 위반).
- 경계가 없으니 규칙을 어디 둘지도 안 보임.

**Week 2의 진짜 미션** = 이 거대 덩어리를 위 지도처럼 **3~4개 Aggregate로 쪼개는 결정을 내리고 문서화**하는 것.
(코드 대공사는 단계적으로. 우선 경계선과 근거부터.)

---

## 6. 외우면 되는 체크리스트

두 엔티티 A, B를 두고:

- [ ] A가 바뀔 때 B도 **같은 트랜잭션에서 반드시** 맞아야 하는 규칙이 있나?
  - YES → 같은 Aggregate. 한쪽을 Root로.
  - NO → 다른 Aggregate.
- [ ] B를 바꾸려면 **A(Root)를 통해서만** 가능한가? (B를 직접 Repository로 안 꺼내는가?)
- [ ] 다른 Aggregate는 **객체가 아니라 ID로** 참조하는가?
- [ ] "조금 늦게 맞아도 되는" 값(좋아요 수 같은)은 Aggregate **밖**에 두고 결과적 일관성으로?

> 🔑 마지막 직관: **"이 둘을 따로 저장했을 때 깨지는 규칙이 있으면 한 묶음, 없으면 남남."**
> 정원 초과는 즉시 막아야 하니 한 묶음(Project). 좋아요/지원은 늦어도 되니 남남.
