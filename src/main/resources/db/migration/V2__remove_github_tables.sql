-- V2: 사용하지 않는 테이블 삭제
-- GitHub 연동, LLM, PR Review 기능 제거로 인해 관련 테이블 삭제

-- GitHub 관련 테이블
DROP TABLE IF EXISTS pull_request_file;
DROP TABLE IF EXISTS pull_request;
DROP TABLE IF EXISTS project_repo;
DROP TABLE IF EXISTS webhook_delivery;

-- LLM 관련 테이블
DROP TABLE IF EXISTS llm_task_result;
DROP TABLE IF EXISTS llm_task;

-- PR Review 관련 테이블
DROP TABLE IF EXISTS pr_review_finding;
DROP TABLE IF EXISTS pr_review_job;
