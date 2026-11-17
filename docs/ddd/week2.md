# Week 2 — "객체에게 일을 시켜라" : 전술적 설계로 리팩터링

> **이번 주 한 줄 목표**
> Week 1에서 찾은 "빈약한" 도메인 하나를 골라, **테스트로 보호한 뒤** 규칙을 객체 안으로 끌어들이고
> Aggregate 경계를 직접 그어본다.

> ⚠️ **철칙: 테스트 없이는 한 줄도 고치지 않는다.** (Week 1에서 "레거시 = 테스트 없는 코드"라고 했다.)
> 리팩터링은 "동작은 그대로, 구조만 개선"이다. 동작이 그대로인지는 **테스트만이 증명**한다.

---

## 0. 왜 이 순서인가 (방향성)

Week 1에서 너는 "규칙이 서비스에 흩어져 있다"는 걸 느꼈다.
Week 2의 임무는 그 규칙을 **제자리(도메인 객체)로 돌려보내는 것**이다. 순서가 중요하다:

```
① 특성화 테스트로 현재 동작 고정  →  ② VO로 값에 의미 부여  →
③ 규칙을 엔티티 메서드 안으로  →  ④ Aggregate 경계 긋기  →  ⑤ Repository/Factory 정리
```

이 순서를 어기면(예: 테스트 없이 Aggregate부터) 반드시 뭔가 깨지고, 깨진 걸 모른 채 넘어간다.

---

## 🧭 큼직한 개념 힌트

- **특성화 테스트(Characterization Test)**
  "이게 옳은 동작이냐"가 아니라 **"지금 이렇게 동작한다"** 를 박제하는 테스트. 리팩터링의 안전망.
- **Entity vs Value Object**
  Entity = 식별자(ID)로 구분, 생명주기 동안 변함. VO = **값 자체가 정체성**, 불변(immutable), 교체만 가능.
  돈·기간·좌표·마감일 → VO 후보. (이미 `project/vo/RecruitmentDeadline` 가 좋은 예다.)
- **Tell, Don't Ask**
  객체의 데이터를 꺼내서(ask) 바깥에서 판단하지 말고, 객체에게 시켜라(tell).
  `if (app.getStatus() == PENDING) app.updateStatus(ACCEPTED)` → `app.accept()`
- **Aggregate / Aggregate Root**
  함께 변해야 하는 객체 묶음 = 일관성 경계. 바깥에서는 **Root를 통해서만** 내부에 접근.
  규칙: ①한 트랜잭션 = 한 Aggregate 변경 ②Aggregate 간엔 **객체 참조 대신 ID 참조** 권장.
- **Repository**
  Aggregate **Root 단위로만** 존재. 내부 구성요소(child)는 자기 Repository를 갖지 않는다.
- **Factory**
  복잡한 Aggregate 생성 규칙을 한곳에 캡슐화 (정적 팩터리 메서드/별도 Factory).

---

## 🎯 미션

### Mission 2-0. 환자 선정 + 안전망 구축

- Week 1 회고에서 고른 "가장 이상한 대상"을 이번 주 환자로 확정한다.
  - 추천 환자: **지원(Application) 도메인** — `ProjectApplication` + `ProjectApplicationServiceImpl`.
    (규칙 누수가 가장 심하고, 상태 전이가 명확해 리팩터링 효과를 체감하기 좋다.)
- 🔧 먼저 **특성화 테스트**를 짠다. 코드를 고치기 **전에**.
  - `apply()` 성공 케이스, 자기 자신 지원 실패, 마감/중복 지원 실패
  - `decide()` 수락 시 ProjectMember 합류 / 거절 시 상태 변화
  - `cancel()` 동작
  - **질문**: 테스트를 짜다 보면 "이걸 검증하려면 너무 많은 걸 mocking해야 하네?"를 느낄 것이다. 그 느낌이 바로 **결합도가 높다는 신호**다. 어디가 그런지 적어라.
- 📄 산출물: 지원 도메인 특성화 테스트 (그린 확인)

### Mission 2-1. VO 발굴 — 값에 의미를 주기

- 🔍 후보를 찾아라: 정원/모집 인원, 마감일, 직군+기술 묶음, 지원 동기(motivation) 등.
  - **질문**: `Project`의 `startDate`/`endDate` 두 `LocalDate`는 따로 떠다닌다. "시작 < 종료"라는 규칙은 누가 지키나? 이 둘을 `ProjectPeriod` 같은 VO로 묶으면 그 규칙을 **VO 생성자에서 강제**할 수 있지 않을까?
- 🔧 작은 VO 하나를 실제로 만들어 적용하라. (TDD로: 잘못된 기간 → 예외 테스트부터)
  - 참고 모범: 이미 있는 `project/vo/RecruitmentDeadline` 가 어떻게 생겼는지 먼저 읽어라.
- **VO 체크 질문**: 내가 만든 VO는 (a)불변인가 (b)값으로 동등성 비교(equals/hashCode)되나 (c)스스로 유효성을 보장하나?

### Mission 2-2. 규칙을 엔티티 안으로 — "물어보지 말고 시켜라"

- 🔧 `ProjectApplication.updateStatus(status)` 를 **의도가 담긴 메서드로 대체**하라.
  - 예상 메서드: `accept()`, `reject(String reason)`, `cancel()`.
  - 각 메서드 **안에서** 전이 규칙을 강제하라: "PENDING이 아니면 예외".
  - 그다음 `updateStatus`를 **private로 내리거나 제거**하고, 서비스가 깨지는 걸 보라. 깨진 곳이 바로 규칙이 새던 곳이다.
- 🔧 `ProjectApplicationServiceImpl.processAcceptance()` 의 로직 중 **순수 도메인 규칙**을 엔티티/도메인서비스로 옮겨라.
  - **질문**: 옮기고 나니 서비스 메서드가 얼마나 짧아졌나? 서비스에 남은 건 무엇인가? (남아야 하는 건 "흐름 조율"이지 "규칙"이 아니다.)
- ✅ 매 이동마다 2-0의 테스트가 **여전히 그린**인지 확인. 빨개지면 멈추고 원인부터.

### Mission 2-3. Aggregate 경계 긋기 — 가장 중요한 결정

`Project`는 지금 `members`, `applications`, `projectSkills`, `projectLikes`, `recruitments` 를 **전부 cascade로** 들고 있다. 이게 다 하나의 Aggregate일까?

- **판단 질문 (각각 답해라)**:
  1. `ProjectLike`(좋아요)는 Project가 저장될 때 **함께 일관성**을 지켜야 하나? 좋아요 1개 누를 때 프로젝트 전체를 로드하는 게 맞나? → Like는 별도 Aggregate가 아닐까?
  2. `ProjectApplication`은 Project 안에 있어야 하나, 밖에 있어야 하나? (지원은 `application` 패키지에 따로 있다 — 이미 분리 신호)
  3. `likeCount` 필드 + `increaseLike()` + 낙관적 락(`@Retry`)이 같이 있다. 만약 Like를 분리하면 이 카운트는 어떻게 일관성을 유지하지? (힌트: 이게 바로 "Aggregate 간 일관성은 결과적 일관성으로"의 실전 사례)
- 🔧 결정을 **문서로** 내려라. 코드 대공사는 아직 하지 말고, 경계선을 그린 그림 + 근거를 남긴다.
  - "무엇이 한 Aggregate인지", "Root는 무엇인지", "어디를 객체참조→ID참조로 바꿀지"
- 📄 산출물: `docs/ddd/aggregates.md`

### Mission 2-4. Repository / Factory 점검

- **질문**: `recruitment/repository/RecruitmentStateRepository` 가 따로 있다. `RecruitmentState`는 독립 Aggregate Root인가, 아니면 Project의 일부인가? 만약 Project의 일부라면 이 Repository는 경계 위반 아닌가?
- 🔧 `Project.create(...)` / `Project.java`가 `ProjectPostCommand`를 import하는 문제(Week 1 발견)를 **Factory 또는 정적 팩터리로** 풀어라.
  - 목표: **엔티티가 서비스 DTO를 모르게** 한다. 조립 책임을 누가 가질지 결정.

---

## ✅ Week 2 체크리스트

안전망:
- [ ] 환자 도메인의 특성화 테스트를 **고치기 전에** 작성하고 그린 확인했다
- [ ] mocking이 과하다고 느낀 지점(결합도 신호)을 1개 이상 기록했다

리팩터링:
- [ ] VO를 **1개 이상** 실제로 만들어 규칙을 생성자에서 강제했다 (불변/동등성/자기검증 확인)
- [ ] `updateStatus` 같은 setter형 메서드를 의도가 담긴 메서드(`accept()` 등)로 바꿨다
- [ ] 규칙을 서비스→엔티티로 옮긴 뒤 서비스 메서드가 짧아진 걸 확인했다
- [ ] 모든 이동 후에도 특성화 테스트가 그린이다

설계 결정:
- [ ] `aggregates.md`에 Aggregate 경계·Root·참조 방식을 **근거와 함께** 적었다
- [ ] `ProjectLike`/`ProjectApplication`이 Project Aggregate에 속하는지 답을 냈다
- [ ] 엔티티가 서비스 DTO를 import하던 문제를 Factory로 해소했다

회고:
- [ ] "규칙을 객체 안에 넣으니 ~가 좋아졌다/나빠졌다"를 한 문단으로 적었다
- [ ] Aggregate 경계를 그으며 가장 고민됐던 결정 1개를 적었다

---

## 🚪 다음 주 예고

Week 3는 시야를 **개별 객체에서 시스템 구조로** 넓힌다.
도메인을 인프라(JPA·S3·세종대 포털)로부터 떼어내고(DIP·ACL), 부수효과를 도메인 이벤트로 정식화하며,
이미 있는 Command/Query 분리를 진짜 CQRS 관점에서 다듬는다.
