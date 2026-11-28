-- V3: member_tech_stack 복합 유니크 제약 및 역방향 인덱스 추가
-- 팀원 찾기 검색을 relational division(GROUP BY/HAVING COUNT) 방식으로 전환하기 위한 선행 작업.
--  - 유니크 (member_id, tech_stack_id): 중복 행 방지 + HAVING COUNT 정확성 보장 + member 측 driving 커버링
--  - 인덱스 (tech_stack_id, member_id): 스택 측 driving(희귀 스택에서 좁혀 들어가는 계획) 커버링

-- 1. 기존 중복 행 제거 (같은 회원-스택 쌍이 여러 번 저장된 경우, 가장 오래된 행만 유지)
DELETE mts1 FROM member_tech_stack mts1
JOIN member_tech_stack mts2
  ON mts1.member_id = mts2.member_id
 AND mts1.tech_stack_id = mts2.tech_stack_id
 AND mts1.member_tech_stack_id > mts2.member_tech_stack_id;

-- 2. 복합 유니크 제약 추가 (존재하지 않는 경우에만)
SET @exist_uk := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member_tech_stack'
    AND INDEX_NAME = 'uk_member_tech_stack_pair'
);

SET @sql_uk = IF(@exist_uk = 0,
    'ALTER TABLE member_tech_stack ADD CONSTRAINT uk_member_tech_stack_pair UNIQUE (member_id, tech_stack_id)',
    'SELECT "uk_member_tech_stack_pair already exists"');
PREPARE stmt FROM @sql_uk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 스택 → 회원 방향 커버링 인덱스 추가 (존재하지 않는 경우에만)
SET @exist_idx := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member_tech_stack'
    AND INDEX_NAME = 'idx_member_tech_stack_stack_member'
);

SET @sql_idx = IF(@exist_idx = 0,
    'CREATE INDEX idx_member_tech_stack_stack_member ON member_tech_stack (tech_stack_id, member_id)',
    'SELECT "idx_member_tech_stack_stack_member already exists"');
PREPARE stmt FROM @sql_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 참고: FK가 자동 생성한 단일 컬럼 인덱스 (member_id), (tech_stack_id)는
-- 위 복합 인덱스들이 각각 선두 컬럼으로 포함하므로 중복이 되지만,
-- FK 인덱스 이름이 환경마다 달라 안전을 위해 이 마이그레이션에서는 제거하지 않는다.
