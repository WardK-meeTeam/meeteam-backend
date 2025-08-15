INSERT IGNORE INTO skill(skill_name)
VALUES ('Java'),
       ('Kotlin'),
       ('Spring'),
       ('Spring Boot'),
       ('JPA'),
       ('Hibernate'),
       ('QueryDSL'),
       ('MySQL'),
       ('PostgreSQL'),
       ('MongoDB'),
       ('Redis'),
       ('Elasticsearch'),
       ('Kafka'),
       ('RabbitMQ'),
       ('AWS'),
       ('Docker'),
       ('Kubernetes'),
       ('Git'),
       ('GitHub Actions'),
       ('Jenkins'),
       ('Gradle'),
       ('Maven'),
       ('JUnit 5'),
       ('Mockito'),
       ('Python'),
       ('Node.js'),
       ('Express'),
       ('React'),
       ('Next.js'),
       ('TypeScript'),
       ('JavaScript'),
       ('HTML/CSS'),
       ('Tailwind CSS'),
       ('Nginx'),
       ('GraphQL'),
       ('gRPC'),
       ('Terraform'),
       ('Ansible'),
       ('Linux'),
       ('OAuth2'),
       ('JWT'),
       ('SSE'),
       ('WebSocket'),
       ('WebFlux'),
       ('Micrometer'),
       ('OpenAPI/Swagger'),
       ('Prometheus'),
       ('Grafana'),
       ('Logstash'),
       ('Kibana');


-- 대분류
INSERT INTO big_category (big_category_id, big_category)
VALUES (1, '기획'),
       (2, '디자인'),
       (3, '프론트엔드'),
       (4, '백엔드'),
       (5, '기타');


-- 소분류 (중복은 무시)
INSERT IGNORE INTO sub_category (big_category_id, sub_category) VALUES
-- 1. 기획
(1,'프로덕트 매니저/오너'),
-- 2. 디자인
(2,'그래픽디자인'),(2,'UI/UX디자인'),(2,'모션 디자인'),(2,'BX/브랜드 디자인'),
-- 3. 프론트엔드 개발
(3,'웹프론트엔드'),(3,'iOS'),(3,'안드로이드'),(3,'크로스플랫폼'),
-- 4. 백엔드 개발
(4,'웹서버'),(4,'AI'),(4,'DBA/빅데이터/DS'),
-- 5. 기타
(5,'기타');


