-- =============================================
-- 성능 테스트용 대량 데이터 생성 SQL (ENUM 타입 사용)
-- 목적: Notification 테이블의 복합 인덱스 성능 측정
-- =============================================

-- 1. Member 테이블 대량 데이터 (10만 명)
INSERT INTO member (
    email, age, password, real_name, store_file_name, gender, birth,
    is_participating, introduction, role, provider, provider_id,
    created_at, edited_at
)
SELECT
    CONCAT('user', LPAD(seq, 6, '0'), '@test.com') as email,
    20 + (seq % 40) as age,  -- 20~59세
    '$2a$10$dummypassword.hash.value.for.testing.purpose' as password,
    CONCAT('사용자', LPAD(seq, 6, '0')) as real_name,
    CASE WHEN seq % 3 = 0 THEN CONCAT('profile_', seq, '.jpg') ELSE NULL END as store_file_name,
    CASE WHEN seq % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END as gender,
    DATE_SUB(CURDATE(), INTERVAL (20 + seq % 40) YEAR) as birth,
    CASE WHEN seq % 10 = 0 THEN FALSE ELSE TRUE END as is_participating,
    CASE WHEN seq % 5 = 0 THEN CONCAT('안녕하세요! ', seq, '번째 사용자입니다.') ELSE NULL END as introduction,
    'USER' as role,
    CASE
        WHEN seq % 10 = 0 THEN 'google'
        WHEN seq % 10 = 1 THEN 'github'
        ELSE NULL
    END as provider,
    CASE
        WHEN seq % 10 <= 1 THEN CONCAT('social_', seq)
        ELSE NULL
    END as provider_id,
    NOW() - INTERVAL (seq % 365) DAY as created_at,
    NOW() - INTERVAL (seq % 365) DAY as edited_at
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 + 1 as seq
    FROM
        (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
) numbers
WHERE seq <= 100000;

-- 2. Project 테이블 대량 데이터 (1만 개)
INSERT INTO project (
    creator_id, project_name, description, project_category, platform_category,
    image_url, offline_required, status, recruitment_status, start_date, end_date,
    is_deleted, like_count, created_at, edited_at
)
SELECT
    1 + (seq % 100000) as creator_id,  -- 랜덤 사용자가 생성
    CONCAT('프로젝트_', LPAD(seq, 5, '0')) as project_name,
    CONCAT('이것은 ', seq, '번째 프로젝트입니다. 함께 개발해요!') as description,
    CASE
        WHEN seq % 8 = 0 THEN 'ENVIRONMENT'
        WHEN seq % 8 = 1 THEN 'PET'
        WHEN seq % 8 = 2 THEN 'HEALTHCARE'
        WHEN seq % 8 = 3 THEN 'EDUCATION'
        WHEN seq % 8 = 4 THEN 'AI_TECH'
        WHEN seq % 8 = 5 THEN 'FASHION_BEAUTY'
        WHEN seq % 8 = 6 THEN 'FINANCE_PRODUCTIVITY'
        ELSE 'ETC'
    END as project_category,
    CASE
        WHEN seq % 3 = 0 THEN 'WEB'
        WHEN seq % 3 = 1 THEN 'ANDROID'
        ELSE 'IOS'
    END as platform_category,
    CASE WHEN seq % 5 = 0 THEN CONCAT('project_', seq, '.jpg') ELSE NULL END as image_url,
    CASE WHEN seq % 4 = 0 THEN TRUE ELSE FALSE END as offline_required,
    CASE
        WHEN seq % 3 = 0 THEN 'PLANNING'
        WHEN seq % 3 = 1 THEN 'ONGOING'
        ELSE 'COMPLETED'
    END as status,
    CASE WHEN seq % 10 = 0 THEN 'CLOSED' ELSE 'RECRUITING' END as recruitment_status,
    CURDATE() - INTERVAL (seq % 365) DAY as start_date,
    CURDATE() + INTERVAL (30 + seq % 180) DAY as end_date,
    CASE WHEN seq % 50 = 0 THEN TRUE ELSE FALSE END as is_deleted,
    seq % 100 as like_count,  -- 0~99 좋아요
    NOW() - INTERVAL (seq % 365) DAY as created_at,
    NOW() - INTERVAL (seq % 365) DAY as edited_at
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + 1 as seq
    FROM
        (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
) numbers
WHERE seq <= 10000;

-- 3. Notification 테이블 대량 데이터 (500만 개)
-- 특정 사용자들에게 집중적으로 알림을 생성하여 인덱스 효과를 극대화
INSERT INTO notification (
    receiver_id, actor_id, application_id, type, message, is_read, project_id, created_at, edited_at
)
SELECT
    -- 80-20 법칙 적용: 20%의 사용자가 80%의 알림을 받도록
    CASE
        WHEN seq % 10 <= 7 THEN 1 + (seq % 20000)      -- 상위 20% 사용자 (1~20,000)가 80% 알림
        ELSE 20001 + (seq % 80000)                      -- 나머지 80% 사용자가 20% 알림
    END as receiver_id,
    1 + (seq % 100000) as actor_id,
    CASE WHEN seq % 3 = 0 THEN 1 + (seq % 50000) ELSE NULL END as application_id,
    CASE
        WHEN seq % 4 = 0 THEN 'PROJECT_APPLY'
        WHEN seq % 4 = 1 THEN 'PROJECT_MY_APPLY'
        WHEN seq % 4 = 2 THEN 'PROJECT_APPROVE'
        ELSE 'PROJECT_REJECT'
    END as type,
    CASE
        WHEN seq % 4 = 0 THEN CONCAT('새로운 지원자가 있습니다. #', seq)
        WHEN seq % 4 = 1 THEN CONCAT('지원이 완료되었습니다. #', seq)
        WHEN seq % 4 = 2 THEN CONCAT('지원이 승인되었습니다. #', seq)
        ELSE CONCAT('지원이 거절되었습니다. #', seq)
    END as message,
    -- 읽음/안읽음 비율을 다양하게 (30% 안읽음, 70% 읽음)
    CASE WHEN seq % 10 <= 2 THEN FALSE ELSE TRUE END as is_read,
    1 + (seq % 10000) as project_id,
    NOW() - INTERVAL (seq % 365) DAY - INTERVAL (seq % 24) HOUR as created_at,
    NOW() - INTERVAL (seq % 365) DAY - INTERVAL (seq % 24) HOUR as edited_at
FROM (
    SELECT
        a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 + f.N * 100000 + 1 as seq
    FROM
        (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
        CROSS JOIN (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) f
) numbers
WHERE seq <= 5000000;

-- =============================================
-- 성능 측정용 쿼리들
-- =============================================

-- 현재 상태 확인
SELECT
    'members' as table_name, COUNT(*) as count FROM member
UNION ALL
SELECT
    'projects' as table_name, COUNT(*) as count FROM project
UNION ALL
SELECT
    'notifications' as table_name, COUNT(*) as count FROM notification;

-- 인덱스 적용 전 성능 측정 쿼리
-- 1. 특정 사용자의 안읽은 알림 수 (가장 많은 알림을 받는 사용자)
SELECT COUNT(*) FROM notification WHERE receiver_id = 1 AND is_read = false;

-- 2. 여러 사용자들의 안읽은 알림 수
SELECT COUNT(*) FROM notification WHERE receiver_id IN (1, 2, 3, 4, 5) AND is_read = false;

-- 3. 특정 기간의 안읽은 알림 수
SELECT COUNT(*) FROM notification
WHERE receiver_id = 1 AND is_read = false
AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 4. 페이징 성능 테스트 (실제 애플리케이션에서 사용하는 쿼리와 유사)
SELECT n.*, m.real_name, p.project_name
FROM notification n
LEFT JOIN member m ON n.receiver_id = m.member_id
LEFT JOIN project p ON n.project_id = p.project_id
WHERE n.receiver_id = 1
ORDER BY n.created_at DESC
LIMIT 20 OFFSET 0;

-- =============================================
-- 복합 인덱스 생성 쿼리
-- =============================================

-- 주 복합 인덱스: 안읽은 알림 수 조회 최적화
CREATE INDEX idx_notification_receiver_read_created
ON notification (receiver_id, is_read, created_at);

-- FileSort 최적화 인덱스: ORDER BY created_at + WHERE actor_id 최적화
CREATE INDEX idx_notification_actor_created
ON notification (actor_id, created_at DESC);

-- FileSort 최적화 인덱스: ORDER BY created_at + WHERE receiver_id 최적화
CREATE INDEX idx_notification_receiver_created_desc
ON notification (receiver_id, created_at DESC);

-- 복합 조건 최적화: WHERE actor_id + WHERE type + ORDER BY created_at
CREATE INDEX idx_notification_actor_type_created
ON notification (actor_id, type, created_at DESC);

-- 복합 조건 최적화: WHERE receiver_id + WHERE type + ORDER BY created_at
CREATE INDEX idx_notification_receiver_type_created
ON notification (receiver_id, type, created_at DESC);

-- 페이징 최적화: WHERE receiver_id + ORDER BY created_at + LIMIT
CREATE INDEX idx_notification_receiver_created_id
ON notification (receiver_id, created_at DESC, id);

-- 인덱스 삭제
DROP INDEX idx_notification_receiver_read_created ON notification;
-- 추가 인덱스 옵션들 (다양한 조합 테스트용)
-- CREATE INDEX idx_notification_receiver_read ON notification (receiver_id, is_read);
-- CREATE INDEX idx_notification_receiver_created ON notification (receiver_id, created_at);
-- CREATE INDEX idx_notification_read_receiver ON notification (is_read, receiver_id);
-- CREATE INDEX idx_notification_created_desc ON notification (created_at DESC);

-- =============================================
-- 인덱스 적용 후 성능 비교 쿼리
-- =============================================

-- 실행 계획 확인 (인덱스 사용 여부 체크)
EXPLAIN SELECT COUNT(*) FROM notification WHERE receiver_id = 1 AND is_read = false;

-- 실행 계획 상세 분석
EXPLAIN FORMAT=JSON SELECT COUNT(*) FROM notification WHERE receiver_id = 1 AND is_read = false;

-- 실제 실행 시간 측정 (여러 번 실행해서 평균 구하기)
SELECT
    receiver_id,
    COUNT(CASE WHEN is_read = false THEN 1 END) as unread_count,
    COUNT(*) as total_count,
    AVG(CASE WHEN is_read = false THEN 1 ELSE 0 END) * 100 as unread_percentage
FROM notification
WHERE receiver_id IN (1, 2, 3, 4, 5, 10, 100, 1000)
GROUP BY receiver_id
ORDER BY unread_count DESC;

-- 페이징 쿼리 성능 비교
SELECT n.id, n.type, n.message, n.is_read, n.created_at,
       m.real_name as actor_name, p.project_name
FROM notification n
LEFT JOIN member m ON n.actor_id = m.member_id
LEFT JOIN project p ON n.project_id = p.project_id
WHERE n.receiver_id = 1
ORDER BY n.created_at DESC
LIMIT 20;

-- 시간 범위별 알림 수 (최근 활동 분석)
SELECT
    DATE(created_at) as date,
    COUNT(*) as total_notifications,
    COUNT(CASE WHEN is_read = false THEN 1 END) as unread_notifications
FROM notification
WHERE receiver_id = 1
AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- =============================================
-- 인덱스 효과 측정용 추가 쿼리
-- =============================================

-- 쿼리 실행 시간 측정 (프로파일링)
SET profiling = 1;
SELECT COUNT(*) FROM notification WHERE receiver_id = 1 AND is_read = false;
SELECT COUNT(*) FROM notification WHERE receiver_id = 100 AND is_read = false;
SELECT COUNT(*) FROM notification WHERE receiver_id = 1000 AND is_read = false;
SHOW PROFILES;

-- 인덱스 사용률 확인
SHOW INDEX FROM notification;

-- 테이블 통계 정보 확인
ANALYZE TABLE notification;
SHOW TABLE STATUS LIKE 'notification';

-- =============================================
-- 정리용 쿼리 (테스트 완료 후 사용)
-- =============================================

-- 인덱스 삭제 (원복용)
-- DROP INDEX idx_notification_receiver_read_created ON notification;

-- 테스트 데이터 삭제 (주의: 실제 데이터도 함께 삭제됨)
-- DELETE FROM notification WHERE id > 0;
-- DELETE FROM project WHERE id > 0;
-- DELETE FROM member WHERE id > 0;

-- AUTO_INCREMENT 초기화
-- ALTER TABLE notification AUTO_INCREMENT = 1;
-- ALTER TABLE project AUTO_INCREMENT = 1;
-- ALTER TABLE member AUTO_INCREMENT = 1;

-- 테이블 상태 최종 확인
-- SELECT TABLE_NAME, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH
-- FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = DATABASE()
-- AND TABLE_NAME IN ('member', 'project', 'notification');

-- =============================================
-- FileSort 최적화 테스트 쿼리들
-- =============================================

-- FileSort 발생 쿼리 (인덱스 적용 전)
-- 1. actor_id 조건 + ORDER BY created_at (FileSort 발생)
SELECT * FROM notification
WHERE actor_id = 1
ORDER BY created_at DESC
LIMIT 10;

-- 2. receiver_id 조건 + ORDER BY created_at (FileSort 발생)
SELECT * FROM notification
WHERE receiver_id = 1
ORDER BY created_at DESC
LIMIT 20;

-- 3. actor_id + type 조건 + ORDER BY created_at (FileSort 발생)
SELECT * FROM notification
WHERE actor_id = 1 AND type = 'PROJECT_APPLY'
ORDER BY created_at DESC
LIMIT 10;

-- 4. receiver_id + type 조건 + ORDER BY created_at (FileSort 발생)
SELECT * FROM notification
WHERE receiver_id = 1 AND type = 'PROJECT_APPLY'
ORDER BY created_at DESC
LIMIT 10;

-- 5. 복잡한 페이징 쿼리 (FileSort 발생)
SELECT n.id, n.type, n.message, n.created_at, n.is_read,
       m.real_name as actor_name, p.project_name
FROM notification n
LEFT JOIN member m ON n.actor_id = m.member_id
LEFT JOIN project p ON n.project_id = p.project_id
WHERE n.receiver_id = 1
ORDER BY n.created_at DESC
LIMIT 20 OFFSET 100;

-- =============================================
-- FileSort 최적화 확인 쿼리들 (인덱스 적용 후)
-- =============================================

-- 실행 계획 확인: FileSort 제거 여부 체크
EXPLAIN SELECT * FROM notification
WHERE actor_id = 1
ORDER BY created_at DESC
LIMIT 10;

EXPLAIN SELECT * FROM notification
WHERE receiver_id = 1
ORDER BY created_at DESC
LIMIT 20;

EXPLAIN SELECT * FROM notification
WHERE actor_id = 1 AND type = 'PROJECT_APPLY'
ORDER BY created_at DESC
LIMIT 10;

EXPLAIN SELECT * FROM notification
WHERE receiver_id = 1 AND type = 'PROJECT_APPLY'
ORDER BY created_at DESC
LIMIT 10;

-- 상세 실행 계획 (JSON 형식으로 FileSort 확인)
EXPLAIN FORMAT=JSON
SELECT * FROM notification
WHERE actor_id = 1
ORDER BY created_at DESC
LIMIT 10;

EXPLAIN FORMAT=JSON
SELECT * FROM notification
WHERE receiver_id = 1
ORDER BY created_at DESC
LIMIT 20;

-- =============================================
-- 성능 비교 측정 쿼리
-- =============================================

-- 프로파일링으로 실행 시간 비교
SET profiling = 1;

-- actor_id 기준 쿼리들
SELECT COUNT(*) FROM notification WHERE actor_id = 1;
SELECT * FROM notification WHERE actor_id = 1 ORDER BY created_at DESC LIMIT 10;
SELECT * FROM notification WHERE actor_id = 1 AND type = 'PROJECT_APPLY' ORDER BY created_at DESC LIMIT 10;

-- receiver_id 기준 쿼리들
SELECT * FROM notification WHERE receiver_id = 1 ORDER BY created_at DESC LIMIT 20;
SELECT * FROM notification WHERE receiver_id = 1 AND type = 'PROJECT_APPLY' ORDER BY created_at DESC LIMIT 10;

-- 페이징 쿼리
SELECT * FROM notification WHERE receiver_id = 1 ORDER BY created_at DESC LIMIT 20 OFFSET 0;
SELECT * FROM notification WHERE receiver_id = 1 ORDER BY created_at DESC LIMIT 20 OFFSET 100;

SHOW PROFILES;

-- =============================================
-- 인덱스 효과 분석 쿼리
-- =============================================

-- 각 사용자별 알림 분포 확인 (인덱스 효과 측정용)
SELECT
    actor_id,
    COUNT(*) as total_notifications,
    COUNT(CASE WHEN type = 'PROJECT_APPLY' THEN 1 END) as apply_notifications,
    MIN(created_at) as earliest_notification,
    MAX(created_at) as latest_notification
FROM notification
WHERE actor_id IN (1, 2, 3, 4, 5, 10, 100, 1000)
GROUP BY actor_id
ORDER BY total_notifications DESC;

-- 시간대별 알림 패턴 분석 (ORDER BY 성능 확인)
SELECT
    DATE(created_at) as notification_date,
    HOUR(created_at) as notification_hour,
    COUNT(*) as notification_count
FROM notification
WHERE actor_id = 1
AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at), HOUR(created_at)
ORDER BY notification_date DESC, notification_hour DESC
LIMIT 50;

-- 타입별 최신 알림 조회 (복합 인덱스 효과 확인)
SELECT type, id, message, created_at
FROM (
    SELECT *,
           ROW_NUMBER() OVER (PARTITION BY type ORDER BY created_at DESC) as rn
    FROM notification
    WHERE actor_id = 1
) ranked
WHERE rn <= 3
ORDER BY type, created_at DESC;

