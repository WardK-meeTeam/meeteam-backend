-- V8: member (deleted_at, project_count) 정렬 인덱스 추가
-- projectCount 정렬 조회(메인페이지, 팀원 검색)의 filesort를 인덱스 순서 스캔으로 대체한다.
-- LIMIT 페이지가 차면 조기 종료할 수 있는 "멤버 주도" 실행 계획을 옵티마이저 선택지에 추가하는 것이 목적.
-- deleted_at을 선행 컬럼으로 두어 deleted_at IS NULL 필터 구간 안에서 project_count 정렬 순서가 유지된다.

SET @exist_idx := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member'
    AND INDEX_NAME = 'idx_member_deleted_project_count'
);

SET @sql_idx = IF(@exist_idx = 0,
    'CREATE INDEX idx_member_deleted_project_count ON member (deleted_at, project_count)',
    'SELECT "idx_member_deleted_project_count already exists"');
PREPARE stmt FROM @sql_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;