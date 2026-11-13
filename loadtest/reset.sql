-- 부하 테스트 데이터 초기화 (재실험용)
-- 핫 프로젝트의 지원서/답변을 삭제하고 누적 지원 수를 0으로 되돌린다.
-- 회원/프로젝트는 유지하므로 reset → k6 재실행만 하면 된다.

SET @pid := (SELECT project_id FROM project WHERE project_name = 'LOADTEST_HOT_PROJECT');

-- 1. 동적 지원서 답변 (EAV) 삭제
DELETE aa FROM application_answer aa
JOIN project_member_application pa ON aa.application_id = pa.project_application_id
WHERE pa.project_id = @pid;

-- 2. 지원 알림 삭제 (리더에게 쌓인 지원 알림)
DELETE n FROM notification n
WHERE n.project_id = @pid;

-- 3. 지원서 삭제
DELETE FROM project_member_application WHERE project_id = @pid;

-- 4. 누적 지원 수 초기화
UPDATE project SET application_count = 0 WHERE project_id = @pid;

SELECT @pid AS hot_project_id, '초기화 완료' AS status;
