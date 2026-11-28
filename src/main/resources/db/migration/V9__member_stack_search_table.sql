-- V9: 기술스택 팀원 검색 전용 투영 테이블 (읽기 모델)
-- 세미조인 검색은 옵티마이저의 전략 선택이 규모에 따라 틀어진다(230k 회원에서 Materialize 51.9ms
-- vs FirstMatch 0.116ms — docs/멤버검색.md 실험 기록). 쿼리 모양에 맞춘 투영 테이블은
-- 스택별 인덱스 순서로 (offset+limit)행만 읽어 어떤 분포에서도 비용 상한이 보장된다.
--
-- 활성 회원의 행만 존재한다(탈퇴 시 행 삭제) — deleted_at 컬럼/필터 없음.
-- 동기화: MemberStackSearchProjector(스택 수정·가입·탈퇴·삭제),
--         MemberRepository 카운터 default 메서드(project_count 미러).
-- 파생 데이터이므로 아래 백필 쿼리로 언제든 전체 재구축 가능.

CREATE TABLE IF NOT EXISTS member_stack_search (
    tech_stack_id  BIGINT NOT NULL,
    project_count  INT    NOT NULL,
    member_id      BIGINT NOT NULL,
    PRIMARY KEY (tech_stack_id, member_id),
    -- 검색 쿼리 전용: WHERE tech_stack_id = ? ORDER BY project_count DESC → 인덱스 순서 스캔 + 조기 종료
    KEY idx_mss_stack_pcount (tech_stack_id, project_count DESC, member_id)
);

-- 백필 (멱등 — 이미 있는 행은 무시)
INSERT IGNORE INTO member_stack_search (tech_stack_id, project_count, member_id)
SELECT mts.tech_stack_id, m.project_count, m.member_id
FROM member_tech_stack mts
JOIN member m ON m.member_id = mts.member_id
WHERE m.deleted_at IS NULL;
