-- V6: member.project_count 비정규화 컬럼 추가 + 기존 데이터 백필
-- 팀원 찾기 projectCount 정렬의 상관 스칼라 서브쿼리(ORDER BY (SELECT COUNT(*) FROM project_member ...))를
-- 컬럼 정렬로 대체하기 위한 선행 작업.
-- 값의 의미: 회원별 project_member 행 수(전부 카운트 — 소프트삭제된 프로젝트 참여도 포함, 기존 동작 유지).

-- 1. 컬럼 추가 (존재하지 않는 경우에만), NOT NULL DEFAULT 0
SET @exist_col := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member'
    AND COLUMN_NAME = 'project_count'
);

SET @sql_col = IF(@exist_col = 0,
    'ALTER TABLE member ADD COLUMN project_count INT NOT NULL DEFAULT 0',
    'SELECT "member.project_count already exists"');
PREPARE stmt FROM @sql_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 기존 데이터 백필 (회원별 project_member 행 수)
-- 주의: member 테이블의 PK 컬럼명은 member_id (엔티티 @Column(name="member_id") private Long id)
UPDATE member m
SET m.project_count = (
    SELECT COUNT(*) FROM project_member pm WHERE pm.member_id = m.member_id
);