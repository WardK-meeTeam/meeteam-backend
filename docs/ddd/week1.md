# Week 1 — "내 코드는 정말 객체지향일까?" : 진단과 언어 통일

> **이번 주 한 줄 목표**
> 코드를 한 줄도 고치지 말고, **내 프로젝트가 데이터 중심인지 도메인 중심인지를 스스로 증명**한다.
> 그리고 팀과 코드가 같은 단어를 쓰고 있는지 확인한다.

> ⚠️ 이번 주는 **리팩터링 금지**. 관찰·기록·질문만 한다. 칼을 들기 전에 환자를 진단하는 주간이다.

---

## 0. 왜 이걸 먼저 하는가 (방향성)

DDD는 패턴 모음이 아니라 **"복잡한 도메인을 코드로 정직하게 옮기는 법"** 이다.
그런데 옮기려면 먼저 두 가지를 알아야 한다.

1. 지금 내 코드가 **현실(비즈니스)을 어떻게 왜곡하고 있는가** → 진단
2. 현실을 **무슨 단어로 부르고 있는가** → 언어

이 두 개 없이 Entity/Aggregate부터 손대면, "패턴은 적용했는데 왜 안 나아지지?"가 된다.
그래서 Week 1은 **느끼는 주간**이다. 정답을 주지 않는다. 네가 직접 냄새를 맡아야 한다.

---

## 🧭 큼직한 개념 힌트

이번 주에 머릿속에 담아둘 큰 그림 (깊이 파지 말고 '감'만):

- **데이터 중심 설계 vs 도메인 중심 설계**
  데이터 중심 = 엔티티는 데이터 자루(getter/setter)일 뿐, 모든 판단/규칙은 Service가 한다.
  도메인 중심 = 규칙을 가진 데이터가 스스로 판단한다. "물어보지 말고 시켜라(Tell, Don't Ask)".
- **빈약한 도메인 모델(Anemic Domain Model)**
  마틴 파울러가 말한 안티패턴. 엔티티에 행위가 없고 Service가 비대해지는 현상.
- **레거시 코드 = 테스트가 없는 코드** (마이클 페더스)
  동작은 하지만 **두려워서 못 고치는** 코드. DDD 리팩터링의 최대 적.
- **유비쿼터스 언어(Ubiquitous Language)**
  기획·대화·코드·DB가 **같은 단어**를 쓰는 것. 번역이 필요한 순간 버그가 숨는다.
- **Bounded Context**
  그 단어가 일관되게 통하는 경계. '멤버'가 회원(Member)인지 프로젝트 참여자(ProjectMember)인지 달라지는 지점.

---

## 🎯 미션

### Mission 1-1. "신의 서비스"를 찾아라 — 규칙이 어디 사는지 추적하기

비즈니스 규칙은 원래 도메인 객체가 알아야 한다. 그런데 지금은 어디 있을까?

- 🔍 단서: `application/service/ProjectApplicationServiceImpl.java` (349줄)
  - `apply()`, `decide()`, `processAcceptance()`, `processRejection()` 를 읽어라.
  - **질문 1**: "지원을 수락하면 무슨 일이 일어나는가?"라는 비즈니스 규칙이 **엔티티에 있나, 서비스에 있나?**
  - **질문 2**: `processAcceptance()` 안의 로직을 `ProjectApplication` 엔티티가 모른 채로 두면, 다른 곳에서 또 수락 처리를 할 때 같은 규칙을 **복붙**하게 되지 않을까?

- 🔍 단서: `application/entity/ProjectApplication.java` 의 `updateStatus(ApplicationStatus status)`
  - **질문 3**: 이 메서드는 외부에서 **아무 상태나** 집어넣을 수 있다. PENDING인 지원만 ACCEPTED가 될 수 있다는 규칙은 누가 지키고 있나? 만약 누가 `updateStatus(CANCELLED)`를 취소 불가 상태에서 호출하면?

> 💡 여기서 "어 이거 엔티티가 자길 못 지키네?"를 느끼면 성공.

### Mission 1-2. setter 추적 — 캡슐화가 뚫린 곳

- 🔍 단서: `member/entity/Member.java` 의 `setIntroduction()`, `setStoreFileName()`
  - 이 setter를 **누가 호출하는지** 전부 찾아라 (`grep`).
  - **질문**: setter로 값을 바꿀 때, 지켜져야 할 규칙(길이 제한? null 금지?)이 있나? 있다면 지금 그 규칙은 어디 있나? 없다면 정말 없어도 되나?

- 같은 파일에서 `withdraw()`, `updateProfile()` 같은 **의도가 담긴 메서드**와 `setXxx()`를 비교하라.
  - **질문**: 둘의 차이가 느껴지나? 어느 쪽이 "이 객체가 뭘 할 수 있는지"를 더 잘 설명하나?

### Mission 1-3. 풍부함과 빈약함의 공존을 증명하라

- 🔍 단서: `project/entity/Project.java` (473줄)
  - 이 엔티티는 `validateLeaderPermission()`, `isRecruiting()`, `rejectAllPendingApplications()` 등 **이미 풍부하다**.
  - **질문**: 왜 `Project`는 풍부한데 `ProjectApplication`·`Member`는 빈약할까? 같은 프로젝트인데 일관성이 없는 이유는? (정답 없음. 네 추측을 적어라. — 보통 "가장 많이 만진 엔티티가 가장 풍부해진다")
  - 😈 함정 발견: `Project.java`가 `import ...project.service.dto.ProjectPostCommand` 를 하고 있다. **엔티티가 서비스 계층의 DTO를 알고 있다.** 이게 왜 냄새인지 적어라. (힌트: 의존 방향)

### Mission 1-4. 용어 사전(Glossary) 초안 만들기

코드를 읽으며 마주친 핵심 명사/동사를 **한글 도메인 용어 ↔ 코드 식별자**로 표에 정리하라.

- 출발 후보: 모집, 지원, 지원자, 모집 상태, 직군, 기술 스택, 프로젝트 멤버, 마감.
- 😈 혼선을 찾아라:
  - `recruitment` 패키지 vs `Recruitment` enum(`recruitmentStatus`) vs `RecruitmentState` 엔티티 — **셋 다 "모집"인데 다 다르다.** 뭐가 뭔지 한 줄씩 정의해봐.
  - `skill` 패키지 vs `TechStack` vs `docs/job-tech-stack.md` — "기술 스택"을 부르는 이름이 통일돼 있나?
- 📄 산출물: `docs/ddd/glossary.md`

### Mission 1-5. Bounded Context 지도 그리기 (손그림 OK)

`domain/` 하위 패키지를 후보 경계로 보고, **누가 누구를 의존하는지** 화살표로 그려라.

- **질문**: `application`(지원)은 `project`·`member` 둘 다 의존한다. 그럼 지원은 독립 컨텍스트인가, 프로젝트의 일부인가?
- 📄 산출물: `docs/ddd/context-map.md` (텍스트 다이어그램도 충분)

---

## ✅ Week 1 체크리스트

진단:
- [ ] 비즈니스 규칙이 서비스에 사는 사례를 **3개 이상** 구체적 위치(파일:메서드)로 적었다
- [ ] `updateStatus` / setter 가 깨뜨릴 수 있는 불변식을 **1개 이상** 글로 설명했다
- [ ] `Project`가 DTO를 import하는 의존 역전 위반을 발견하고 이유를 적었다
- [ ] "내 프로젝트는 데이터 중심인가?"에 대해 **증거를 들어** 한 문단으로 답했다

언어:
- [ ] `glossary.md` 에 핵심 용어 **10개 이상** 정리
- [ ] "모집"을 가리키는 3가지 이름(recruitment/Recruitment/RecruitmentState)의 차이를 구분해 적었다
- [ ] `context-map.md` 에 도메인 간 의존 화살표를 그렸다

회고 (자유 서술):
- [ ] 이번 주 가장 "어 이거 이상한데?" 했던 순간 1개를 적었다
- [ ] Week 2에서 가장 먼저 손대고 싶은 대상 1개를 골랐다

---

## 🚪 다음 주 예고

Week 2는 드디어 **칼을 든다**. 단, 그냥 고치는 게 아니라 **테스트로 보호한 뒤** Entity·VO·Aggregate로 리팩터링한다.
이번 주에 네가 "가장 이상하다"고 느낀 그 대상이 Week 2의 첫 환자다.
