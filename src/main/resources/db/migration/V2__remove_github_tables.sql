-- V2: GitHub 관련 테이블 삭제
-- GitHub 연동 기능 제거로 인해 관련 테이블 삭제

-- pull_request_file 테이블 삭제 (pull_request에 의존)
DROP TABLE IF EXISTS pull_request_file;

-- pull_request 테이블 삭제 (project_repo에 의존)
DROP TABLE IF EXISTS pull_request;

-- project_repo 테이블 삭제
DROP TABLE IF EXISTS project_repo;

-- webhook_delivery 테이블 삭제
DROP TABLE IF EXISTS webhook_delivery;
