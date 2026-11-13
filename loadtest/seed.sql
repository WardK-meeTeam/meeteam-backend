-- 부하 테스트 시드 데이터 (로컬 MySQL 전용)
-- 생성물: 지원자 회원 N명 + 리더 1명 + 핫 프로젝트 1개 + WEB_FRONTEND 모집 상태 1개
-- 회원 이메일 규칙: loadtest00001@loadtest.local ~ loadtestNNNNN@loadtest.local
--   → LoadTestTokenGenerator 가 같은 규칙으로 토큰을 발급하므로 둘의 개수를 맞출 것.
-- 재실행해도 안전(이미 있으면 스킵). 지원서만 초기화하려면 reset.sql 사용.

SET @member_count := 20000;
SET SESSION cte_max_recursion_depth = 1000000;

-- 1. 직군/직무 (없으면 생성)
INSERT INTO job_field (code, name)
SELECT 'FRONTEND', '프론트엔드'
WHERE NOT EXISTS (SELECT 1 FROM job_field WHERE code = 'FRONTEND');

INSERT INTO job_position (job_field_id, code, name)
SELECT jf.job_field_id, 'WEB_FRONTEND', '웹 프론트엔드'
FROM job_field jf
WHERE jf.code = 'FRONTEND'
  AND NOT EXISTS (SELECT 1 FROM job_position WHERE code = 'WEB_FRONTEND');

-- 2. 프로젝트 리더 회원
INSERT INTO member (email, password, real_name, role, project_count, version, created_at, edited_at)
SELECT 'loadtest-leader@loadtest.local', 'loadtest', '부하테스트리더', 'USER', 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM member WHERE email = 'loadtest-leader@loadtest.local');

-- 3. 지원자 회원 N명 (증분 삽입: 이미 있는 수만큼 건너뛰므로 @member_count를 늘려 재실행하면 된다)
SET @existing := (SELECT COUNT(*) FROM member WHERE email LIKE 'loadtest_____@loadtest.local');

INSERT INTO member (email, password, real_name, role, project_count, version, created_at, edited_at)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @member_count
)
SELECT CONCAT('loadtest', LPAD(n, 5, '0'), '@loadtest.local'),
       'loadtest',
       CONCAT('부하테스터', n),
       'USER', 0, 0, NOW(), NOW()
FROM seq
WHERE n > @existing;

-- 4. 핫 프로젝트 (모두가 여기에 지원 → project 행 락 경합 유발 지점)
INSERT INTO project (creator_id, project_name, description, recruitment_status,
                     recruitment_deadline_type, is_deleted, like_count, application_count,
                     created_at, edited_at)
SELECT m.member_id, 'LOADTEST_HOT_PROJECT', '락 경합 부하 테스트용 프로젝트', 'RECRUITING',
       'RECRUITMENT_COMPLETED', 0, 0, 0, NOW(), NOW()
FROM member m
WHERE m.email = 'loadtest-leader@loadtest.local'
  AND NOT EXISTS (SELECT 1 FROM project WHERE project_name = 'LOADTEST_HOT_PROJECT');

-- 5. 모집 상태 (WEB_FRONTEND, 마감되지 않도록 설정)
INSERT INTO recruitment_state (project_id, job_position_id, recruit_count, current_count, is_closed)
SELECT p.project_id, jp.job_position_id, 10, 0, 0
FROM project p
JOIN job_position jp ON jp.code = 'WEB_FRONTEND'
WHERE p.project_name = 'LOADTEST_HOT_PROJECT'
  AND NOT EXISTS (
      SELECT 1 FROM recruitment_state rs WHERE rs.project_id = p.project_id
  );

-- 6. 확인: k6 실행 시 PROJECT_ID 로 넘길 값
SELECT p.project_id AS hot_project_id,
       (SELECT COUNT(*) FROM member WHERE email LIKE 'loadtest%@loadtest.local') AS loadtest_members
FROM project p
WHERE p.project_name = 'LOADTEST_HOT_PROJECT';