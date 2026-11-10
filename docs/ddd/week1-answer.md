# Week 1 — 나의 답 + 피드백

> 형식: 각 질문마다 **🙋 나의 생각**(원문 그대로) → **🧑‍🏫 피드백**.
> 피드백은 "정답 통보"가 아니라 "네 생각을 어디까지 밀어붙일 수 있는지"를 보여주는 용도다.

---

## 질문 1. "지원을 수락하면 무슨 일이 일어나는가"는 엔티티에 있나, 서비스에 있나?

### 🙋 나의 생각
```java
projectMemberService.addMember(projectId, applicantId, application.getJobPosition());
```
이 메서드는 지원을 수락하면 처리하는 서비스를 따로 호출한다. 굳이 호출을 해야 할까?
엔티티 내부에서 수락하는 과정을 처리할 수도 있을 것 같다. 명확한 방식은 생각하지 못하겠다.
객체 간의 협력으로 풀어나갈 수 있지 않을까?

### 🧑‍🏫 피드백 — 방향은 맞다. 단, "전부 엔티티로"는 함정이다.

먼저 `processAcceptance()`가 실제로 하는 일을 **종류별로** 분류해보자. 이게 핵심이다.

| 하는 일 | 종류 | 누가 해야 하나 |
|---|---|---|
| `updateStatus(ACCEPTED)` | **도메인 규칙** (지원의 상태 전이) | `ProjectApplication` 자신 |
| 정원 +1 (`increaseCurrentCount`) | **도메인 규칙** (모집 상태 일관성) | `RecruitmentState` / `Project` |
| 모집 상태 갱신 (`updateRecruitmentsStatus`) | **도메인 규칙** | `Project` |
| 알림 저장 + 이벤트 발행 | **부수효과(side effect)** | Application Service |
| 응답 DTO 조립 | **표현(presentation)** | Application Service |

여기서 깨달아야 할 것:

> **"수락하면 일어나는 일"은 하나의 덩어리가 아니다.**
> 도메인 규칙은 도메인으로, 부수효과·조율은 서비스로 가야 한다.

그래서 네가 느낀 "엔티티 내부에서 처리하고 싶다"는 직관은 **규칙 부분에 대해서만 맞다.** 하지만 한 가지 함정:

😈 **`processAcceptance`는 세 개의 Aggregate를 건드린다.** — `ProjectApplication`, `ProjectMember`(+`Project`), `RecruitmentState`.
DDD 규칙상 **하나의 Aggregate가 다른 Aggregate의 내부를 직접 바꾸면 안 된다.** 그래서 "지원이 자기 안에서 멤버까지 추가"하게 만들면 오히려 경계를 부순다.

**그럼 정답에 가까운 그림은?** — "물어보지 말고 시켜라"를 각 객체에 적용하고, 서비스는 **조율자**로 남긴다:

```java
// Application Service = 흐름 조율 (규칙 없음)
application.accept();                      // ① 지원 스스로 상태 전이 + 규칙 검증
project.acceptMember(applicant, jobPos);   // ② 프로젝트가 멤버 합류 + 정원/상태를 책임
eventPublisher.publish(new ApplicationAccepted(...));  // ③ 부수효과는 이벤트로
```

서비스에는 `if/else`나 `status == PENDING` 같은 **판단이 사라지고**, 도메인 객체에게 "시키는" 줄만 남는다.
이게 네가 말한 **"객체 간의 협력"** 의 구체적 모습이다. 👍

> 🔑 한 문장 요약: 엔티티로 "다 옮기는" 게 아니라, **규칙은 각 Aggregate에게 나눠 시키고, 서비스는 순서만 조율**한다.

---

## 질문 2. 규칙을 엔티티가 모르면, 다른 곳에서 또 수락 처리 시 복붙하게 되지 않나?

### 🙋 나의 생각
맞다. 그래서 엔티티 내부에서 수락 처리를 하는 게 좋아 보인다. 하지만 수락 시 인원 추가할 때 validation 등등
검증할 것이 많아서 자연스럽게 서비스 코드에 붙인 것 같다. 더 도메인 자체로 위임할 수 있으면 반복 사용이 가능해 보인다.

### 🧑‍🏫 피드백 — 정확히 핵심을 짚었다. "왜 서비스에 붙었는지"까지 본 게 좋다.

"검증할 게 많아서 자연스럽게 서비스에 붙었다" — 이게 **빈약한 모델이 태어나는 전형적 경로**다.
검증이 많다는 건 엔티티로 옮기지 **않을** 이유가 아니라, 오히려 옮겨야 할 이유다. 지금 검증이 어디 흩어져 있는지 보자:

```java
// validateDecisionPrecondition - 서비스에 있음
if (application.getProject().isCompleted()) throw ...;      // ← Project가 알아야 할 규칙
if (application.getStatus() != PENDING) throw ...;          // ← Application이 알아야 할 규칙
```

두 검증 모두 **남의 데이터를 꺼내서(`getStatus()`, `isCompleted()`) 바깥에서 판단**한다. (= "물어보기, ask")
이걸 각 객체 안으로 넣으면:

```java
public void accept() {
    if (this.status != PENDING)
        throw new CustomException(APPLICATION_ALREADY_DECIDED);  // 지원이 스스로 지킴
    this.status = ACCEPTED;
}
```

이제 `accept()`는 **어디서 호출하든** 같은 규칙이 보장된다. 네가 말한 "반복 사용 가능" = 규칙의 **단일 출처(Single Source of Truth)**. 정확하다.

⚠️ 단, 질문 1의 교훈을 잊지 마라: `isCompleted()` 체크는 Application이 아니라 **Project**의 규칙이다.
"프로젝트가 완료됐으면 지원을 수락할 수 없다"는 Project Aggregate가 지켜야 한다. **검증을 옮기되, 올바른 주인에게** 옮겨라.

> 🔑 빈약한 모델은 "규칙이 많아서"가 아니라 "규칙을 **주인 아닌 곳(서비스)** 에 둬서" 생긴다.

---

## 질문 3. 왜 Project는 풍부한데 Application·Member는 빈약할까?

### 🙋 나의 생각
Project가 도메인의 중심에 있기 때문에 가장 많이 사용해야 했다. 사용 빈도가 많아지고 참조하는 곳이 많아지니
자연스럽게 많은 역할을 담당하게 되어 풍부해졌다. 더 좋은 방법은 모르겠다. 순수하게 Project가 많이 쓰이니까.

### 🧑‍🏫 피드백 — 관찰은 정확. 단, 이건 "결과"지 "바람직한 설계"는 아니다.

"많이 만진 엔티티가 풍부해진다"는 **사실**이다. 하지만 이건 설계가 아니라 **퇴적(accretion)** 이다.
즉, 의도해서 그렇게 된 게 아니라 손이 자주 가다 보니 **우연히** 쌓인 것. 그래서 두 가지 부작용이 생긴다:

1. **Project가 너무 풍부해진다 (God Entity)** — 473줄. `Project`가 모집·멤버·좋아요·지원 상태를 다 알면, 정작 `RecruitmentState`나 `ProjectApplication`이 가져야 할 규칙까지 Project가 빨아들인다.
2. **나머지는 빈약한 채 방치된다** — 손이 덜 가니까.

**올바른 기준은 "사용 빈도"가 아니라 "책임(소유권)"이다.**

> 규칙은 그 **데이터를 소유한 객체**에 둔다. (정보 전문가 패턴 / Information Expert)

- 지원 상태 전이 규칙 → `status`를 가진 `ProjectApplication`
- 정원 차감/충원 규칙 → `currentCount`를 가진 `RecruitmentState`
- 멤버 합류 일관성 → 멤버 목록을 가진 `Project`

이렇게 나누면 Project는 **적당히** 풍부해지고(자기 책임만), 나머지도 **자기 몫만큼** 풍부해진다.
"Project처럼 다 풍부하게"가 목표가 아니라, **책임을 골고루 분배**하는 게 목표다. 이게 Week 2 Aggregate 미션의 핵심이 된다.

---

## 함정. Project가 서비스 DTO(ProjectPostCommand)를 import하는 게 왜 냄새인가?

### 🙋 나의 생각
양방향 의존관계이다. 의존관계라 함은 변경이 발생할 때 의존하는 클래스에서도 변경할 가능성이 있다는 것이다.
결국 양방향 의존관계라 함은 서로 변경이 발생할 경우 서로 영향을 받는다.
이 부분은 DTO를 넘기지 말고 필드를 넘겨서 엔티티의 일관성을 유지하자. 어디서도 사용할 수 있도록.

### 🧑‍🏫 피드백 — 결론(필드를 넘겨라)은 정답. 단, 진단 용어를 하나 고치자.

🔧 **"양방향 의존"이 아니다.** 정확히는 **"방향이 틀린 단방향 의존"** 이다. 차이가 중요하다.

의존 방향은 항상 **안쪽(도메인)을 향해야** 한다:

```
web/presentation  →  service/application  →  domain   ✅ (바깥이 안을 의존)
```

그런데 지금은:
```
domain(Project)  →  service(ProjectPostCommand)        ❌ (안이 바깥을 의존)
```

DTO가 Project를 모르더라도(즉 단방향이라도) **이미 잘못**이다. 도메인이 자기보다 바깥 계층을 알아버렸기 때문.
왜 위험한가:

- **재사용 불가**: Project를 다른 입력(예: 외부 API, 배치)으로 만들고 싶어도 `ProjectPostCommand`가 강제된다.
- **변경 전파**: 서비스 계층 사정(웹 요청 모양)이 바뀌면 도메인이 흔들린다. 도메인은 가장 안정적이어야 하는데 가장 불안정한 것에 묶였다.
- **테스트 어려움**: 순수 도메인 테스트에 서비스 계층 클래스가 끌려온다.

✅ 네 해결책("필드를 넘겨라")이 정확하다. `Project.create(name, description, ...)` 또는 **정적 팩터리/Factory**로 조립해서, 엔티티가 DTO를 모르게 한다. (Week 2 Mission 2-4)

> 🔑 한 문장: 단방향이든 양방향이든, **화살표가 도메인에서 바깥으로 나가면** 그게 냄새다.

---

## 질문(Context Map). 지원은 독립 컨텍스트인가, 프로젝트의 일부인가?

### 🙋 나의 생각
지원은 어쩔 수 없이 project와도 의존하고 member에도 의존할 수밖에 없다고 생각한다.
독립 컨텍스트라 함은 의존관계 없는 도메인을 의미하는 건가? 위에서 질문한 세 개의 엔티티는 의존할 수밖에 없지 않나?

### 🧑‍🏫 피드백 — 여기서 개념 하나를 바로잡아야 한다. 좋은 질문이다.

🔧 **"독립 컨텍스트 = 의존이 없는 것"이 아니다.** 이게 가장 흔한 오해다.

> 세상 모든 도메인은 서로 의존한다. **의존이 0인 컨텍스트는 없다.**
> Bounded Context의 "독립"은 **"자기만의 모델과 언어, 일관성 경계를 소유하느냐"** 의 문제다.

비유: 회사의 두 팀(영업팀, 회계팀)은 **협력**한다(의존한다). 그래도 각자 **독립된 팀**이다. 각자 자기 용어·자기 책임·자기 규칙을 갖기 때문. 컨텍스트도 똑같다. 핵심은 **"의존하느냐"가 아니라 "어떻게 의존하느냐(관계의 형태)"** 다.

그래서 "지원이 독립 컨텍스트냐"는 이렇게 다시 물어야 한다:

- 지원은 **자기만의 규칙/생명주기**가 있나? → 있다 (PENDING→ACCEPTED/REJECTED/CANCELLED 상태 전이, 중복 지원 금지, 자기 자신 지원 금지...). ✅ 독립 컨텍스트 자격 충분.
- 그럼 project/member에 **어떻게** 의존해야 하나? → 여기서 두 단계를 구분하라:

**① Bounded Context 레벨(전략적, 굵은 경계):**
지원은 독립 컨텍스트로 보는 게 맞다. 이미 `application` 패키지로 분리돼 있는 것도 그 신호.

**② Aggregate 레벨(전술적, 가는 경계):**
지금 `ProjectApplication`은 `Project`와 `Member`를 **객체로 직접 참조**한다(`application.getProject().getCreator()...`).
DDD는 Aggregate 간에는 **객체 참조 대신 ID 참조**를 권한다:

```java
// 지금
private Project project;        // 객체 참조 → Project 전체가 딸려옴
private Member applicant;

// 지향 (Week 2~3에서 고민)
private Long projectId;         // ID 참조 → 경계가 명확, 결합 느슨
private Long applicantId;
```

`application.getProject().getCreator().getEmail()` 같은 **법칙 위반(디미터 법칙)** 체인이 지금 서비스에 보이는 건, 객체 참조로 남의 Aggregate 속을 헤집고 있다는 증거다.

> 🔑 정리:
> - 독립 컨텍스트 = 의존 없음 ❌ / 자기 모델·규칙·경계를 소유함 ✅
> - 지원은 **독립 컨텍스트**로 보되, project·member와는 **느슨하게(ID 참조 + 정의된 관계)** 협력한다.
> - "의존을 없앨까?"가 아니라 "의존을 **어떤 모양으로** 만들까?"가 진짜 질문이다.

---

## 🎯 종합 피드백 — 네 사고의 강점과 다음 숙제

**강점 (이미 잘하고 있는 것):**
- 규칙이 서비스에 샌다는 걸 **코드 줄로** 짚었다 (`addMember` 호출, `updateStatus`).
- "왜 그렇게 됐는지"(검증이 많아서, 많이 써서)까지 **원인을 추적**했다. 진단가의 자세.
- DTO 문제의 **해결책**(필드를 넘겨라)을 스스로 냈다.

**다음 주로 가져갈 교정 포인트 3개:**
1. "엔티티로 다 옮기기"가 아니라 **"규칙을 올바른 주인에게 나눠주기"**. (God Entity 경계)
2. 의존 진단 용어: 양방향이 아니라 **방향이 틀린** 의존.
3. Bounded Context "독립" = 의존 없음이 아니라 **모델·경계의 소유**. 협력은 ID 참조로 느슨하게.

**Week 2 첫 환자 추천:** `ProjectApplication`.
- `updateStatus(status)` → `accept()` / `reject()` / `cancel()` 로 쪼개고, 전이 규칙을 안으로.
- `processAcceptance`의 검증(`isCompleted`, `status==PENDING`)을 **각 주인 객체**로 이사.
- 그 과정에서 "이건 Application 규칙? Project 규칙?"을 매번 물어라. 그게 Aggregate 경계 감각을 길러준다.

> 마지막 한마디: 너는 지금 "어떻게 고치지?"보다 **"왜 이렇게 됐지?"** 를 잘 본다. 그게 DDD에서 더 귀한 능력이다.
> 패턴은 외우면 되지만, 냄새를 맡는 코는 훈련으로만 길러진다. 잘하고 있다. 🙂