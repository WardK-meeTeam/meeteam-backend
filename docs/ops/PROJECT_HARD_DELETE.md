# 프로젝트 레코드 수동 삭제 런북 (Hard Delete)

> 특정 `project` 레코드를 DB에서 **물리적으로 삭제**해야 할 때의 안전한 절차입니다.
> 연관 테이블(FK)이 많아 순서를 지키지 않으면 외래키 제약에 막힙니다.

---

## ⚠️ 먼저 읽어주세요

1. **이 앱은 원래 소프트 삭제(`is_deleted = true`)를 사용합니다** (`ProjectCommandServiceImpl.delete()`).
   단순히 "그 프로젝트를 숨기고 싶은" 거라면 아래 [소프트 삭제](#소프트-삭제-권장-기본)를 쓰세요. 데이터·정합성이 안전합니다.
2. 물리 삭제는 **복구 불가**입니다. 반드시 **트랜잭션**으로 감싸고, 삭제 전 **영향 건수 조회**로 범위를 확인하세요.
3. JPA의 `CascadeType.ALL`은 **앱(EntityManager)으로 지울 때만** 동작합니다. **DB에는 `ON DELETE CASCADE`가 없으므로**, raw SQL은 자식부터 직접 지워야 합니다.

---

## 대상 스키마

- 부모 테이블: `project` (PK: `project_id`)
- 소프트 삭제 컬럼: `is_deleted` (boolean)

### FK 의존성 트리 (자식 → 부모)

```
project (project_id)
├── project_member              (project_id)
├── project_member_application  (project_id)
├── project_skill               (project_id)
├── project_like                (project_id)
├── notification                (project_id)     ← 앱에서도 cascade 안 됨, 항상 수동
├── recruitment_state           (project_id)
│      └── recruitment_tech_stack (recruitment_state_id)   ← 손자: 먼저 삭제
└── project_qna                 (project_id)
       └── qna_answer            (qna_id)                  ← 손자: 먼저 삭제
```

> 채팅/리뷰 등 다른 도메인은 `project`를 FK로 참조하지 않음(2026-06-15 기준 엔티티 매핑 확인).

---

## 0단계. 실제 FK 목록 재확인 (스키마 변경 대비 권장)

코드 기준 트리이므로, 실행 전 **DB에 직접 물어** 누락이 없는지 확인하면 100% 확실합니다.

```sql
-- project 를 직접 참조하는 모든 FK
SELECT table_name AS 자식테이블, column_name AS FK컬럼, constraint_name AS 제약
FROM information_schema.key_column_usage
WHERE referenced_table_name = 'project'
  AND table_schema = DATABASE();

-- 1단계 자식(project_qna, recruitment_state ...)을 참조하는 손자 FK
SELECT table_name AS 자식테이블, column_name AS FK컬럼, referenced_table_name AS 부모
FROM information_schema.key_column_usage
WHERE referenced_table_name IN ('project_qna','recruitment_state','project_member','project_member_application')
  AND table_schema = DATABASE();
```

위 결과가 아래 삭제 스크립트의 테이블 목록과 일치하는지 대조하세요. (새 테이블이 추가됐다면 같은 규칙으로 순서에 끼워 넣습니다.)

---

## 1단계. 삭제 전 영향 범위 확인

```sql
SET @pid = 123;   -- ← 삭제할 project_id

SELECT 'project'                    AS tbl, COUNT(*) c FROM project                    WHERE project_id = @pid
UNION ALL SELECT 'project_member',            COUNT(*) FROM project_member             WHERE project_id = @pid
UNION ALL SELECT 'project_member_application', COUNT(*) FROM project_member_application WHERE project_id = @pid
UNION ALL SELECT 'project_skill',             COUNT(*) FROM project_skill              WHERE project_id = @pid
UNION ALL SELECT 'project_like',              COUNT(*) FROM project_like               WHERE project_id = @pid
UNION ALL SELECT 'notification',              COUNT(*) FROM notification               WHERE project_id = @pid
UNION ALL SELECT 'recruitment_state',         COUNT(*) FROM recruitment_state          WHERE project_id = @pid
UNION ALL SELECT 'project_qna',               COUNT(*) FROM project_qna                WHERE project_id = @pid
UNION ALL SELECT 'recruitment_tech_stack',    COUNT(*) FROM recruitment_tech_stack
          WHERE recruitment_state_id IN (SELECT recruitment_state_id FROM recruitment_state WHERE project_id = @pid)
UNION ALL SELECT 'qna_answer',                COUNT(*) FROM qna_answer
          WHERE qna_id IN (SELECT qna_id FROM project_qna WHERE project_id = @pid);
```

`project` 행이 정확히 1이고, 나머지 건수가 예상 범위인지 확인합니다.

---

## 2단계. 삭제 실행 (트랜잭션)

```sql
START TRANSACTION;

SET @pid = 123;   -- ← 삭제할 project_id (1단계와 동일 값)

-- (A) 손자 먼저
DELETE FROM qna_answer
 WHERE qna_id IN (SELECT qna_id FROM project_qna WHERE project_id = @pid);

DELETE FROM recruitment_tech_stack
 WHERE recruitment_state_id IN (SELECT recruitment_state_id FROM recruitment_state WHERE project_id = @pid);

-- (B) 직접 자식 (서로 참조 없음 → 순서 무관)
DELETE FROM project_qna                 WHERE project_id = @pid;
DELETE FROM recruitment_state           WHERE project_id = @pid;
DELETE FROM project_member              WHERE project_id = @pid;
DELETE FROM project_member_application  WHERE project_id = @pid;
DELETE FROM project_skill               WHERE project_id = @pid;
DELETE FROM project_like                WHERE project_id = @pid;
DELETE FROM notification                WHERE project_id = @pid;

-- (C) 마지막에 부모
DELETE FROM project                     WHERE project_id = @pid;

-- 결과 확인 후 결정:
-- COMMIT;     -- 정상이면
-- ROLLBACK;  -- 이상하면
```

> 권장: 먼저 `ROLLBACK` 전제로 한 번 실행해 각 `DELETE`의 영향 행 수를 본 뒤, 문제없으면 다시 실행해 `COMMIT` 합니다.

---

## 소프트 삭제 (권장 기본)

물리 삭제가 꼭 필요한 게 아니라면 이걸 쓰세요. 앱 로직과 동일하게 동작하고 안전합니다.

```sql
UPDATE project SET is_deleted = true WHERE project_id = 123;
```

---

## 체크리스트

- [ ] 정말 물리 삭제가 필요한가? (아니면 `is_deleted = true`)
- [ ] 0단계로 실제 FK 목록을 확인했는가? (스키마가 바뀌었을 수 있음)
- [ ] 1단계로 영향 건수를 확인했는가? (`project` = 1)
- [ ] `START TRANSACTION`으로 감쌌는가?
- [ ] 삭제 후 결과를 보고 `COMMIT` / `ROLLBACK` 판단했는가?
- [ ] 운영 DB라면 백업/스냅샷이 있는가?

---

## 변경 이력
- 2026-06-15: 최초 작성. 엔티티 매핑(`domain/project` 외) 기준 FK 트리 도출.
  스키마는 Hibernate가 생성하므로(JPA 매핑이 FK의 근거), 매핑 변경 시 이 문서의 트리/순서도 갱신할 것.