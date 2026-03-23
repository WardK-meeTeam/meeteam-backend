# Blue/Green 무중단 배포 구현 계획

## 개요

현재 배포 방식에서 발생하는 다운타임을 제거하기 위해 기존 Nginx + Docker Compose 기반 Blue/Green 배포를 구현합니다.

### 현재 인프라 상태

- **Nginx**: EC2 호스트에 설치됨 (systemd 관리)
- **HTTPS**: Let's Encrypt 인증서 적용 완료
- **도메인**: `api.meeteam.alom-sejong.com`
- **프록시**: `127.0.0.1:8080`으로 직접 연결 중

### 목표 아키텍처

```
                    ┌─────────────────┐
                    │  Client (443)   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Nginx (Host)   │
                    │  HTTPS 적용됨   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │    upstream     │
                    │ meeteam_backend │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
     ┌────────▼────────┐          ┌────────▼────────┐
     │   Blue (8080)   │          │  Green (8081)   │
     │   meeteam-blue  │          │  meeteam-green  │
     └─────────────────┘          └─────────────────┘
              │                             │
              └──────────────┬──────────────┘
                             │
                    ┌────────▼────────┐
                    │     Redis       │
                    └─────────────────┘
```

### 배포 흐름

1. 현재 활성 환경 확인 (Blue or Green)
2. 비활성 환경에 새 이미지 배포
3. Health check 통과 대기
4. Nginx upstream 전환 (무중단)
5. 구버전 컨테이너 종료

---

## 단계별 작업

### 1단계: Nginx 설정 수정 (EC2 서버)

#### 1.1 upstream 설정 파일 생성

**파일**: `/etc/nginx/conf.d/meeteam-upstream.conf`

```nginx
# Blue/Green 배포를 위한 upstream 설정
# 배포 스크립트가 이 파일을 동적으로 변경합니다

upstream meeteam_backend {
    server 127.0.0.1:8080;  # Blue (초기값)
    # server 127.0.0.1:8081;  # Green
}
```

#### 1.2 사이트 설정 수정

**파일**: `/etc/nginx/sites-available/meeteam`

```nginx
server {
    if ($host = api.meeteam.alom-sejong.com) {
        return 301 https://$host$request_uri;
    }

    listen 80;
    server_name api.meeteam.alom-sejong.com;
    return 404;
}

server {
    listen 443 ssl;
    server_name api.meeteam.alom-sejong.com;

    # TLS (기존 설정 유지)
    ssl_certificate /etc/letsencrypt/live/api.meeteam.alom-sejong.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.meeteam.alom-sejong.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # 모든 요청을 upstream으로 전달 (변경됨)
    location / {
        proxy_pass http://meeteam_backend;  # upstream 사용
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_buffering on;
    }

    # SSE 엔드포인트
    location /api/subscribe {
        proxy_pass http://meeteam_backend;  # upstream 사용

        proxy_buffering off;
        chunked_transfer_encoding off;
        proxy_cache off;
        proxy_read_timeout 1h;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### 1.3 작업 명령어

```bash
# 1. upstream 설정 파일 생성
sudo tee /etc/nginx/conf.d/meeteam-upstream.conf << 'EOF'
upstream meeteam_backend {
    server 127.0.0.1:8080;
}
EOF

# 2. 기존 설정 백업
sudo cp /etc/nginx/sites-available/meeteam /etc/nginx/sites-available/meeteam.backup

# 3. 새 설정 적용 (위 내용으로 수정)
sudo nano /etc/nginx/sites-available/meeteam

# 4. 심볼릭 링크 확인/생성
sudo ln -sf /etc/nginx/sites-available/meeteam /etc/nginx/sites-enabled/meeteam

# 5. 기존 .save 파일 제거 (충돌 방지)
sudo rm -f /etc/nginx/sites-enabled/meeteam.save

# 6. 설정 테스트
sudo nginx -t

# 7. Nginx 리로드
sudo systemctl reload nginx
```

#### 체크리스트
- [ ] `/etc/nginx/conf.d/meeteam-upstream.conf` 생성
- [ ] `/etc/nginx/sites-available/meeteam` 수정 (proxy_pass → upstream)
- [ ] 기존 `.save` 파일 정리
- [ ] `nginx -t` 테스트 통과
- [ ] `systemctl reload nginx` 실행

---

### 2단계: Docker Compose 수정

#### 2.1 compose.yml 수정

**파일**: `compose.yml`

```yaml
services:
    meeteam-blue:
        image: 079892728769.dkr.ecr.ap-northeast-2.amazonaws.com/meeteam-server:latest
        container_name: meeteam-blue
        ports:
            - "8080:8080"
        env_file:
            - .env
        environment:
            - SPRING_PROFILES_ACTIVE=prod
        depends_on:
            redis:
                condition: service_healthy
        volumes:
            - ./files:/app/files
        networks:
            - meeteam-network
        healthcheck:
            test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
            interval: 10s
            timeout: 5s
            retries: 3
            start_period: 40s
        restart: unless-stopped

    meeteam-green:
        image: 079892728769.dkr.ecr.ap-northeast-2.amazonaws.com/meeteam-server:latest
        container_name: meeteam-green
        ports:
            - "8081:8080"
        env_file:
            - .env
        environment:
            - SPRING_PROFILES_ACTIVE=prod
        depends_on:
            redis:
                condition: service_healthy
        volumes:
            - ./files:/app/files
        networks:
            - meeteam-network
        healthcheck:
            test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
            interval: 10s
            timeout: 5s
            retries: 3
            start_period: 40s
        restart: unless-stopped

    redis:
        image: redis:7
        container_name: redis
        command: ["redis-server", "--appendonly", "yes"]
        ports:
            - "6379:6379"
        volumes:
            - ./redis_data:/data
        healthcheck:
            test: ["CMD", "redis-cli", "ping"]
            interval: 5s
            retries: 20
        networks:
            - meeteam-network
        restart: always

    prometheus:
        image: prom/prometheus:latest
        container_name: prometheus
        ports:
            - "9090:9090"
        volumes:
            - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
        command:
            - "--config.file=/etc/prometheus/prometheus.yml"
        networks:
            - meeteam-network

    grafana:
        image: grafana/grafana:latest
        container_name: grafana
        ports:
            - "3000:3000"
        depends_on:
            - prometheus
        environment:
            - GF_SECURITY_ADMIN_USER=admin
            - GF_SECURITY_ADMIN_PASSWORD=admin
        volumes:
            - grafana-storage:/var/lib/grafana
        networks:
            - meeteam-network

volumes:
    grafana-storage:
    redis_data:

networks:
    meeteam-network:
        driver: bridge
```

#### 체크리스트
- [ ] compose.yml 파일 수정
- [ ] Blue 컨테이너: 포트 8080:8080
- [ ] Green 컨테이너: 포트 8081:8080
- [ ] healthcheck 설정 추가

---

### 3단계: Health Check 설정 확인

#### 3.1 Actuator 설정 확인

Spring Boot Actuator health 엔드포인트가 노출되어 있는지 확인합니다.

**파일**: `src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info
  endpoint:
    health:
      show-details: never  # 보안상 상세 정보 숨김
```

#### 3.2 테스트

```bash
# 로컬 또는 서버에서 테스트
curl http://localhost:8080/actuator/health
# 응답: {"status":"UP"}
```

#### 체크리스트
- [ ] application-prod.yml에서 actuator health 노출 확인
- [ ] `/actuator/health` 응답 테스트

---

### 4단계: 배포 스크립트 작성

#### 4.1 상태 저장

**파일 기반**: `/home/ubuntu/meeteam/active_env`
- 내용: `blue` 또는 `green`
- 배포 스크립트가 이 파일을 읽고 반대 환경에 배포

#### 4.2 배포 스크립트

**파일**: `/home/ubuntu/meeteam/scripts/deploy.sh`

```bash
#!/bin/bash
set -e

# 설정
DEPLOY_DIR="/home/ubuntu/meeteam"
ACTIVE_ENV_FILE="$DEPLOY_DIR/active_env"
UPSTREAM_CONF="/etc/nginx/conf.d/meeteam-upstream.conf"
MAX_RETRY=30
RETRY_INTERVAL=5

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 현재 활성 환경 확인
get_active_env() {
    if [ -f "$ACTIVE_ENV_FILE" ]; then
        cat "$ACTIVE_ENV_FILE"
    else
        echo "blue"
    fi
}

# 반대 환경 반환
get_inactive_env() {
    if [ "$(get_active_env)" == "blue" ]; then
        echo "green"
    else
        echo "blue"
    fi
}

# 포트 반환
get_port() {
    if [ "$1" == "blue" ]; then
        echo "8080"
    else
        echo "8081"
    fi
}

# Health check 수행
health_check() {
    local port=$1
    local retry=0

    log_info "Health check 시작: localhost:$port"

    while [ $retry -lt $MAX_RETRY ]; do
        if curl -sf http://localhost:$port/actuator/health > /dev/null 2>&1; then
            log_info "Health check 성공!"
            return 0
        fi
        retry=$((retry + 1))
        log_warn "Health check 재시도 ($retry/$MAX_RETRY)..."
        sleep $RETRY_INTERVAL
    done

    log_error "Health check 실패"
    return 1
}

# Nginx upstream 전환
switch_upstream() {
    local target_port=$1

    log_info "Nginx upstream 전환: 127.0.0.1:$target_port"

    sudo tee "$UPSTREAM_CONF" > /dev/null << EOF
upstream meeteam_backend {
    server 127.0.0.1:$target_port;
}
EOF

    # Nginx 설정 테스트
    if ! sudo nginx -t > /dev/null 2>&1; then
        log_error "Nginx 설정 오류"
        return 1
    fi

    # Nginx 리로드 (무중단)
    sudo systemctl reload nginx

    log_info "Upstream 전환 완료"
}

# 메인 배포 로직
deploy() {
    local active_env=$(get_active_env)
    local target_env=$(get_inactive_env)
    local target_port=$(get_port $target_env)
    local target_container="meeteam-$target_env"

    log_info "=========================================="
    log_info "Blue/Green 배포 시작"
    log_info "현재 활성: $active_env"
    log_info "배포 대상: $target_env (port: $target_port)"
    log_info "=========================================="

    cd "$DEPLOY_DIR"

    # 1. 새 이미지 Pull
    log_info "새 이미지 Pull 중..."
    docker compose pull $target_container

    # 2. 대상 컨테이너 시작
    log_info "컨테이너 시작: $target_container"
    docker compose up -d $target_container

    # 3. Health check
    if ! health_check "$target_port"; then
        log_error "배포 실패 - 컨테이너 중지"
        docker compose stop $target_container
        exit 1
    fi

    # 4. Nginx upstream 전환
    switch_upstream "$target_port"

    # 5. 상태 파일 업데이트
    echo "$target_env" > "$ACTIVE_ENV_FILE"
    log_info "활성 환경 업데이트: $target_env"

    # 6. 이전 컨테이너 종료
    local old_container="meeteam-$active_env"
    log_info "이전 컨테이너 종료: $old_container"
    docker compose stop $old_container

    log_info "=========================================="
    log_info "배포 완료!"
    log_info "활성 환경: $target_env"
    log_info "=========================================="
}

# 스크립트 실행
deploy
```

#### 4.3 롤백 스크립트

**파일**: `/home/ubuntu/meeteam/scripts/rollback.sh`

```bash
#!/bin/bash
set -e

# 설정
DEPLOY_DIR="/home/ubuntu/meeteam"
ACTIVE_ENV_FILE="$DEPLOY_DIR/active_env"
UPSTREAM_CONF="/etc/nginx/conf.d/meeteam-upstream.conf"

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 포트 반환
get_port() {
    if [ "$1" == "blue" ]; then
        echo "8080"
    else
        echo "8081"
    fi
}

# 현재 활성 환경 확인
get_active_env() {
    if [ -f "$ACTIVE_ENV_FILE" ]; then
        cat "$ACTIVE_ENV_FILE"
    else
        echo "blue"
    fi
}

# 롤백 수행
rollback() {
    local active_env=$(get_active_env)
    local rollback_env

    if [ "$active_env" == "blue" ]; then
        rollback_env="green"
    else
        rollback_env="blue"
    fi

    local rollback_port=$(get_port $rollback_env)
    local rollback_container="meeteam-$rollback_env"

    log_info "=========================================="
    log_info "롤백 시작"
    log_info "현재 활성: $active_env"
    log_info "롤백 대상: $rollback_env"
    log_info "=========================================="

    cd "$DEPLOY_DIR"

    # 1. 롤백 대상 컨테이너가 실행 중인지 확인
    if ! docker ps --format '{{.Names}}' | grep -q "^$rollback_container$"; then
        log_info "롤백 대상 컨테이너 시작: $rollback_container"
        docker compose up -d $rollback_container
        sleep 10
    fi

    # 2. Health check
    log_info "Health check 수행 중..."
    if ! curl -sf http://localhost:$rollback_port/actuator/health > /dev/null 2>&1; then
        log_error "롤백 대상 컨테이너가 정상이 아닙니다"
        exit 1
    fi

    # 3. Nginx upstream 전환
    log_info "Nginx upstream 전환: 127.0.0.1:$rollback_port"
    sudo tee "$UPSTREAM_CONF" > /dev/null << EOF
upstream meeteam_backend {
    server 127.0.0.1:$rollback_port;
}
EOF
    sudo systemctl reload nginx

    # 4. 상태 파일 업데이트
    echo "$rollback_env" > "$ACTIVE_ENV_FILE"

    # 5. 이전 활성 컨테이너 종료
    log_info "이전 컨테이너 종료: meeteam-$active_env"
    docker compose stop "meeteam-$active_env"

    log_info "=========================================="
    log_info "롤백 완료!"
    log_info "활성 환경: $rollback_env"
    log_info "=========================================="
}

rollback
```

#### 4.4 스크립트 배포 명령어

```bash
# EC2 서버에서 실행
mkdir -p /home/ubuntu/meeteam/scripts

# deploy.sh 생성 (위 내용 복사)
nano /home/ubuntu/meeteam/scripts/deploy.sh

# rollback.sh 생성 (위 내용 복사)
nano /home/ubuntu/meeteam/scripts/rollback.sh

# 실행 권한 부여
chmod +x /home/ubuntu/meeteam/scripts/deploy.sh
chmod +x /home/ubuntu/meeteam/scripts/rollback.sh

# 초기 상태 파일 생성
echo "blue" > /home/ubuntu/meeteam/active_env
```

#### 체크리스트
- [ ] scripts 디렉토리 생성
- [ ] deploy.sh 파일 생성
- [ ] rollback.sh 파일 생성
- [ ] 스크립트 실행 권한 부여
- [ ] active_env 초기 파일 생성 (`echo "blue" > active_env`)

---

### 5단계: GitHub Actions 수정

#### 5.1 deploy.yml 수정

**파일**: `.github/workflows/deploy.yml`

```yaml
name: Deploy to EC2 (Blue/Green)

on:
  push:
    branches:
      - master

jobs:
  Deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Github Repository 파일 불러오기
        uses: actions/checkout@v4

      - name: JDK 17버전 설치
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew

      - name: 테스트 및 빌드하기
        run: ./gradlew clean build -x test

      - name: AWS Resource에 접근할 수 있게 AWS credentials 설정
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-region: ap-northeast-2
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}

      - name: ECR에 로그인
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Docker 이미지 생성
        run: docker build -t meeteam-server .

      - name: Docker 이미지에 Tag 붙이기
        run: docker tag meeteam-server ${{ steps.login-ecr.outputs.registry }}/meeteam-server:latest

      - name: ECR에 Docker 이미지 Push
        run: docker push ${{ steps.login-ecr.outputs.registry }}/meeteam-server:latest

      - name: ECR 토큰(비밀번호) 획득
        id: ecr-token
        run: echo "password=$(aws ecr get-login-password --region ap-northeast-2)" >> $GITHUB_OUTPUT

      - name: SSH로 EC2에 접속하여 Blue/Green 배포
        uses: appleboy/ssh-action@v1.0.3
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_PASSWORD: ${{ steps.ecr-token.outputs.password }}
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_PRIVATE_KEY }}
          envs: ECR_REGISTRY,ECR_PASSWORD
          script_stop: true
          script: |
            set -e
            cd /home/ubuntu/meeteam
            echo $ECR_PASSWORD | docker login --username AWS --password-stdin $ECR_REGISTRY
            ./scripts/deploy.sh
```

#### 체크리스트
- [ ] .github/workflows/deploy.yml 수정

---

### 6단계: 테스트 및 검증

#### 6.1 초기 배포 (Blue 환경)

```bash
# EC2 서버에서 실행

# 1. 먼저 기존 컨테이너 정리
cd /home/ubuntu/meeteam
docker compose down

# 2. 새 compose.yml 적용 (Blue만 시작)
docker compose up -d meeteam-blue redis

# 3. 상태 확인
docker ps
curl http://localhost:8080/actuator/health

# 4. active_env 확인
cat active_env  # blue
```

#### 6.2 Green 전환 테스트

```bash
# 배포 스크립트 실행 (Blue → Green)
./scripts/deploy.sh

# 결과 확인
cat active_env  # green
docker ps       # meeteam-green 실행 중
curl http://localhost:8081/actuator/health
```

#### 6.3 무중단 배포 검증

```bash
# 터미널 1: 지속적인 요청 발생
while true; do
    curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" \
        https://api.meeteam.alom-sejong.com/actuator/health
    sleep 0.5
done

# 터미널 2: 배포 실행
./scripts/deploy.sh

# 결과: 터미널 1에서 502/503 에러 없이 200 응답 지속되면 성공
```

#### 6.4 롤백 테스트

```bash
# 롤백 실행
./scripts/rollback.sh

# 결과 확인
cat active_env
docker ps
```

#### 체크리스트
- [ ] 초기 Blue 환경 배포
- [ ] Green 전환 테스트
- [ ] 무중단 확인 (요청 중 배포)
- [ ] 롤백 테스트
- [ ] GitHub Actions 전체 플로우 테스트

---

## 디렉토리 구조 (최종)

```
/home/ubuntu/meeteam/
├── compose.yml
├── .env
├── active_env                    # blue 또는 green
├── scripts/
│   ├── deploy.sh                 # 배포 스크립트
│   └── rollback.sh               # 롤백 스크립트
├── files/                        # 업로드 파일
├── redis_data/
└── prometheus/
    └── prometheus.yml

/etc/nginx/
├── nginx.conf
├── conf.d/
│   └── meeteam-upstream.conf     # upstream 설정 (동적 변경)
└── sites-available/
    └── meeteam                   # 사이트 설정
```

---

## 작업 요약 체크리스트

### EC2 서버 작업
- [ ] Nginx upstream 설정 파일 생성 (`/etc/nginx/conf.d/meeteam-upstream.conf`)
- [ ] Nginx 사이트 설정 수정 (`proxy_pass` → `upstream`)
- [ ] 기존 `.save` 파일 정리
- [ ] scripts 디렉토리 및 배포 스크립트 생성
- [ ] `active_env` 초기 파일 생성
- [ ] compose.yml 업데이트 (Blue/Green 컨테이너)

### 코드 저장소 작업
- [ ] compose.yml 수정 및 커밋
- [ ] GitHub Actions 워크플로우 수정

### 테스트
- [ ] 초기 Blue 환경 배포
- [ ] Green 전환 테스트
- [ ] 무중단 확인
- [ ] 롤백 테스트
- [ ] GitHub Actions 전체 플로우 테스트

---

## 트러블슈팅

### Health check 실패 시
```bash
# 컨테이너 로그 확인
docker logs meeteam-blue
docker logs meeteam-green

# 직접 health check
curl -v http://localhost:8080/actuator/health
curl -v http://localhost:8081/actuator/health
```

### Nginx 설정 오류 시
```bash
# 설정 테스트
sudo nginx -t

# 설정 문법 확인
sudo nginx -T | grep -A5 "upstream"
```

### 배포 후 502 에러 시
```bash
# upstream 설정 확인
cat /etc/nginx/conf.d/meeteam-upstream.conf

# 대상 포트 컨테이너 실행 확인
docker ps | grep meeteam

# Nginx 리로드
sudo systemctl reload nginx
```