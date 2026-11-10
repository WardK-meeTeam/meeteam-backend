# EAV의 운영 부담 — "value가 타입 없는 문자열"에서 비롯되는 비용

> 동적 지원서를 EAV로 구현한 현재 코드(`application_answer.value VARCHAR`)에서
> **EAV 본질 때문에** 생기는 운영 부담 3가지를 실제 코드와 함께 정리합니다.
> 관련 설계: [[DYNAMIC_APPLICATION_EAV_DESIGN.md]]

## 한 줄 뿌리

세 문제 모두 뿌리가 같습니다:

> **값(`value`)과 선택지(`options`)를 타입 없는 문자열(VARCHAR)에 담는다.**
> → DB가 스키마/타입/제약을 하나도 보장하지 못한다.
> → 정확성 보장이 전부 **애플리케이션 코드로**, 그것도 **여러 곳에, 깨지기 쉽게** 밀려난다.

| 문제 | 뿌리 | 결과 |
| --- | --- | --- |
| ② CAST 취약성 | 숫자도 VARCHAR → 비교하려면 CAST | 한 행이 전체 쿼리를 죽임 |
| ③ 검증 분산 | DB가 타입을 안 막음 → 앱이 전담 | 같은 규칙이 3곳에 중복 |
| ⑤ 선택지 CSV | 리스트를 문자열에 욱여넣음 | 구분자 충돌로 데이터 깨짐 |

---

## ② CAST 취약성 — 한 행이 검색 전체를 죽인다

### 현상

숫자 질문(NUMBER) 필터는 `value`(VARCHAR)를 숫자로 **CAST**해야 올바르게 비교됩니다.
`ApplicationSearchRepositoryImpl`:

```java
switch (filter.operator()) {
    // NUMBER: 문자열을 숫자로 CAST 해야 올바르게 비교됨 (인덱스 미사용)
    case GTE -> jpql.append(" AND cast(ans").append(i).append(".value as integer) >= :val").append(i);
    case LTE -> jpql.append(" AND cast(ans").append(i).append(".value as integer) <= :val").append(i);
    ...
}
```

문제는 `cast(value as integer)`가 **행 단위로 실행**된다는 점입니다.
숫자 질문의 답변 중 **단 한 행이라도** 정수로 변환 불가능하면 → DB가 CAST에서 예외를 던지고 →
**그 프로젝트의 검색 요청 전체가 실패(500)** 합니다.

> 타입 컬럼(`INT`)이면 애초에 잘못된 값이 저장될 수 없어 이런 일이 없습니다.
> EAV는 "문자열이면 다 저장 OK"라서, 한 행의 오염이 쿼리 전체를 무너뜨립니다.

### 이미 어긋나 있는 부분 (쓰기 검증 ↔ 쿼리 가정)

쓰기 시 타입 검증은 이렇게 합니다. `ProjectApplicationServiceImpl#validateAnswerType`:

```java
case NUMBER -> {
    if (!value.matches("-?\\d+")) {                 // 자릿수 제한이 없음!
        throw new CustomException(ErrorCode.APPLICATION_ANSWER_INVALID_TYPE);
    }
}
```

- 정규식 `-?\d+`는 `"99999999999"`(11자리)도 **통과**시킵니다.
- 그런데 쿼리는 `cast(value as integer)` → **int 범위(약 21억) 초과 → 오버플로 예외 → 검색 실패.**
- 즉 **"쓰기가 허용한 값"과 "쿼리가 감당 가능한 값"이 이미 어긋나** 있습니다.
  (쓰기는 `BIGINT` 급을 허용, 쿼리는 `integer`로 가정)

### 왜 EAV 본질인가

숫자가 **전용 타입 컬럼**이 아니라 **공유 VARCHAR**에 들어가기 때문입니다.
그래서 (1) 비교할 때마다 CAST가 필요하고, (2) CAST는 데이터 오염/범위에 취약하며,
(3) 쓰기 검증과 쿼리 가정을 **사람이 영원히 일치**시켜야 합니다.

### 완화

```sql
-- 타입 컬럼이면: DB가 쓰기 때 거부 + CAST 불필요 + 범위 보장
ALTER TABLE ... ADD COLUMN hours INT;           -- 와이드 컬럼
-- 또는 JSON + generated column
ALTER TABLE ... ADD COLUMN hours INT AS (answers->>'$.hours') STORED;
```

- 타입 컬럼/generated column이면 **잘못된 값 자체가 못 들어오고**, CAST도 필요 없습니다.
- EAV 안에서 버티려면 최소한 **쓰기 정규식과 쿼리 타입을 일치**(`integer` 범위로 자릿수 제한)시키고,
  CAST 실패를 대비한 방어 로직이 필요합니다. — 결국 **앱이 DB 몫까지 떠안음.**

---

## ③ 검증 분산 — 같은 규칙이 3곳에 중복된다

### 현상

DB가 타입을 안 막아주므로, **"NUMBER면 숫자", "SELECT면 선택지 안의 값"** 같은 규칙을
애플리케이션이 전담합니다. 그런데 그 규칙이 **최소 3곳**에 흩어져 있습니다.

**(1) 쓰기 검증** — `ProjectApplicationServiceImpl#validateAnswerType`:

```java
case NUMBER -> {
    if (!value.matches("-?\\d+")) throw new CustomException(ErrorCode.APPLICATION_ANSWER_INVALID_TYPE);
}
case SELECT -> {
    if (!question.getOptions().contains(value)) throw new CustomException(ErrorCode.APPLICATION_ANSWER_INVALID_OPTION);
}
```

**(2) 검색 검증** — `ProjectApplicationServiceImpl#validateFilters`:

```java
case GTE, LTE -> {
    // 여기서도 "NUMBER + 숫자"를 또 검증한다
    if (question.getInputType() != QuestionInputType.NUMBER || !filter.value().matches("-?\\d+")) {
        throw new CustomException(ErrorCode.APPLICATION_FILTER_INVALID);
    }
}
```

**(3) 프론트엔드** — `ProjectApplyPage.tsx`:

```tsx
if (question.inputType === 'NUMBER' && answer && Number.isNaN(Number(answer))) {
    return true; // 숫자 아님 → 에러
}
```

→ **"NUMBER는 숫자다"라는 단 하나의 규칙이 Java 쓰기 / Java 검색 / TypeScript UI 3곳에 복제**돼 있습니다.

### 왜 EAV 본질인가

타입 컬럼이면 DB가 `INT`로 **한 번에, 모든 경로에서** 강제합니다.
EAV는 DB 안전망이 0이라, **모든 경계(쓰기·검색·UI)에서 앱이 매번 다시 검증**해야 합니다.
질문 타입이 하나 늘면(`DATE`, `URL` 등) → **세 곳을 동시에** 고쳐야 하고,
하나라도 빠뜨리면 조용히 어긋납니다. (예: 프론트만 고치고 검색 검증을 빼먹으면 이상한 필터가 통과)

### 완화

- **근본**: 타입 컬럼으로 가면 DB가 검증을 흡수 → 중복 상당수 소멸.
- **EAV 안에서**: 타입별 규칙을 **한 곳(QuestionInputType에 전략/검증 메서드)으로 모아** 쓰기·검색이 같은 코드를 호출하게 하면, 최소한 **백엔드 내부 중복(1)(2)**은 제거할 수 있습니다. (프론트 중복은 남음)

```java
// 예: 검증을 타입 enum 한 곳으로 모으기
public enum QuestionInputType {
    NUMBER { public boolean isValid(String v, List<String> opts) { return v.matches("-?\\d{1,9}"); } },
    SELECT { public boolean isValid(String v, List<String> opts) { return opts.contains(v); } },
    TEXT   { public boolean isValid(String v, List<String> opts) { return true; } },
    TEXTAREA { public boolean isValid(String v, List<String> opts) { return true; } };
    public abstract boolean isValid(String value, List<String> options);
}
// 쓰기·검색이 모두 inputType.isValid(...) 하나만 호출 → 중복/드리프트 감소
```

---

## ⑤ 선택지 CSV — 리스트를 문자열에 욱여넣어 깨진다

### 현상

SELECT 선택지(`options`)를 **쉼표로 이은 문자열** 한 컬럼에 저장합니다.
`ApplicationQuestion`:

```java
// 저장: ["Java","Kotlin"] → "Java,Kotlin"
this.optionsCsv = (options == null || options.isEmpty()) ? null : String.join(",", options);

// 조회: "Java,Kotlin" → ["Java","Kotlin"]
public List<String> getOptions() {
    if (optionsCsv == null || optionsCsv.isBlank()) return List.of();
    return List.of(optionsCsv.split(","));
}
```

문제: **선택지 값에 쉼표가 들어가면 round-trip이 깨집니다.**

```java
// 리더가 "Java, Spring" 을 하나의 선택지로 입력하면
String.join(",", List.of("Java, Spring", "Node"))   // → "Java, Spring,Node"
"Java, Spring,Node".split(",")                       // → ["Java", " Spring", "Node"]  ❌ 3개로 쪼개짐
```

- 구분자(쉼표) **이스케이프가 없어** 값에 쉼표가 있으면 개수·내용이 깨집니다.
- 추가로: 앞뒤 공백 처리, 빈 문자열, 중복 선택지 같은 엣지도 코드가 떠안아야 함.

### 왜 EAV 본질인가

이것도 ②③와 **같은 병**입니다 — **구조화된 데이터(리스트)를 타입 없는 문자열에 담는 것.**
DB에 "이건 문자열 배열"이라는 타입이 없으니, 직렬화/역직렬화와 구분자 안전성을
전부 앱이 책임지고, 그 과정에서 데이터가 깨질 틈이 생깁니다.

### 완화

```java
// (A) JSON 컬럼 — 배열을 타입 그대로 저장 (구분자 문제 없음)
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "options", columnDefinition = "json")
private List<String> options;
```

```sql
-- (B) 정규화 — 선택지를 별도 1:N 테이블로 (가장 정석)
CREATE TABLE application_question_option (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    value       VARCHAR(200) NOT NULL,
    sort_order  INT NOT NULL,
    FOREIGN KEY (question_id) REFERENCES application_question(id)
);
```

- **(A) JSON**: 배열을 타입째 저장 → 구분자·이스케이프 문제 소멸. 가장 적은 변경.
- **(B) 정규화**: 선택지로 조회·제약까지 필요하면 정석. 테이블/조인은 늘어남.
- **CSV 유지 시**: 최소한 **안전한 구분자(제어문자)나 이스케이프**가 필요. — 또 앱이 떠안는 일.

---

## 종합

| # | 문제 | 코드 위치 | EAV 본질 | 완화 |
| --- | --- | --- | --- | --- |
| ② | CAST가 행마다 터질 수 있음 + 쓰기/쿼리 범위 불일치 | `ApplicationSearchRepositoryImpl`, `validateAnswerType` | 숫자도 공유 VARCHAR → CAST 필수·취약 | 타입 컬럼 / generated column |
| ③ | "NUMBER=숫자" 규칙이 3곳 중복 | `validateAnswerType`, `validateFilters`, `ProjectApplyPage.tsx` | DB가 타입 강제 못 함 → 앱이 전 경계에서 전담 | 타입 컬럼(DB 강제) / 검증을 타입 enum 한 곳으로 |
| ⑤ | 선택지 CSV 구분자 충돌 | `ApplicationQuestion#optionsCsv` | 리스트를 타입 없는 문자열에 저장 | JSON 배열 / 정규화 테이블 |

**결론:** ②③⑤는 서로 다른 증상이지만 **같은 뿌리** — *"데이터를 타입 없는 문자열에 담아 DB의 스키마/타입/제약을 포기한다"* — 에서 나옵니다.
그래서 정확성·일관성 보장이 전부 앱으로, 여러 곳에, 깨지기 쉽게 밀려납니다.
이 부담을 줄이는 방향이 **타입 컬럼 / JSON+generated column / 정규화**이며,
다음 단계인 **JSON 전환**이 특히 ②⑤를 크게 완화합니다.