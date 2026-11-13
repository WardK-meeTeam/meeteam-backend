-- V7: project.application_count 비정규화 컬럼 추가 + 기존 데이터 백필
-- 프로젝트 지원 시 단일 행 UPDATE(+1)로 누적 지원 수를 관리한다.
-- 값의 의미: 프로젝트별 누적 지원 수(취소/거절 포함 전체 project_member_application 행 수).
-- 지원 트랜잭션 안에서 project 행 X-lock 경합을 재현하기 위한 실험 목적의 컬럼이며,
-- 이후 슬롯 분산 카운터 테이블로 대체를 검토한다.

-- 1. 컬럼 추가 (존재하지 않는 경우에만), NOT NULL DEFAULT 0
SET @exist_col := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project'
    AND COLUMN_NAME = 'application_count'
);

SET @sql_col = IF(@exist_col = 0,
    'ALTER TABLE project ADD COLUMN application_count INT NOT NULL DEFAULT 0',
    'SELECT "project.application_count already exists"');
PREPARE stmt FROM @sql_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 기존 데이터 백필 (프로젝트별 지원서 행 수)
UPDATE project p
SET p.application_count = (
    SELECT COUNT(*) FROM project_member_application pa WHERE pa.project_id = p.project_id
);