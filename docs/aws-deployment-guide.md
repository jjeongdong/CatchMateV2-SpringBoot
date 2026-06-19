# CatchMate AWS Deployment Guide

이 문서는 CatchMate 백엔드 시스템을 AWS(Amazon Web Services) 환경에 배포하기 위한 상세 가이드를 제공합니다. 현재 프로젝트의 Docker 및 Nginx 설정을 기반으로 실무적인 배포 절차를 설명합니다.

---

## 1. 아키텍처 개요

*   **Compute**: AWS EC2 (t3.medium 권장 - 빌드 및 실행 메모리 확보)
*   **Database**: AWS RDS (MySQL 8.0)
*   **Cache/PubSub**: Redis (Docker Compose로 실행)
*   **Storage**: AWS S3 (프로필 이미지 및 파일 업로드)
*   **Reverse Proxy**: Nginx (Docker Compose로 실행)
*   **CI/CD**: GitHub Actions

---

## 2. 사전 준비 사항

### 2.1 AWS 리소스 생성
1.  **EC2 인스턴스**:
    *   OS: Ubuntu 22.04 LTS
    *   보안 그룹 설정:
        *   80 (HTTP) - Open to All
        *   443 (HTTPS) - Open to All
        *   22 (SSH) - My IP Only
2.  **RDS 생성**:
    *   MySQL 8.0 선택
    *   보안 그룹: EC2의 보안 그룹에서 3306 포트 접근 허용

---

## 3. 서버 환경 설정 (EC2)

SSH로 EC2에 접속한 후 다음 명령어를 실행하여 필수 소프트웨어를 설치합니다.

```bash
# 패키지 업데이트
sudo apt-get update && sudo apt-get upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

---

## 4. CI/CD 파이프라인 설정 (GitHub Actions)

`.github/workflows/deploy.yml` 파일(이미 존재함)을 기반으로 GitHub Repository Secret을 설정해야 합니다.

### 설정해야 할 Secret 변수
*   `DOCKER_USERNAME`: DockerHub ID
*   `DOCKER_PASSWORD`: DockerHub Password/Token
*   `HOST_ID`: EC2 인스턴스 퍼블릭 IP
*   `SSH_KEY`: EC2 접속용 .pem 키 내용 전체
*   `ENV_PROPERTIES`: `application-prod.yml` 파일 내용 전체

---

## 5. 배포 절차

### 5.1 Docker 컨테이너 구성
프로젝트 루트의 `docker-compose.yml`을 사용하여 다음과 같이 구성합니다.

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    env_file: .env
    depends_on:
      - redis
      - nginx

  redis:
    image: redis:latest
    ports:
      - "6379:6379"
    volumes:
      - ./redis/data:/data
    command: redis-server --appendonly yes

  nginx:
    image: nginx:latest
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./nginx/certs:/etc/nginx/certs # SSL 인증서 위치
```

### 5.2 SSL 인증서 설정 (Certbot)
무료 SSL 인증서(Let's Encrypt) 발급 방법:

```bash
sudo apt-get install certbot
sudo certbot certonly --manual -d yourdomain.com
# 발급된 인증서(.pem)를 nginx/certs 폴더로 복사
```

---

## 6. 어플리케이션 설정 (Production)

`src/main/resources/application-prod.yml` 파일을 생성하고 운영 환경에 맞는 값을 설정합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-rds-endpoint:3306/catchmate?rewriteBatchedStatements=true
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
  data:
    redis:
      host: redis # Docker 서비스 이름 사용
      port: 6379

cors:
  allowed-origins: https://your-vercel-domain.vercel.app
```

---

## 7. 배포 확인 및 모니터링

1.  **컨테이너 상태 확인**: `docker ps`
2.  **로그 확인**: `docker logs -f [container_id]`
3.  **상태 점검**: `https://yourdomain.com/actuator/health` (추가 시)

---

## 8. 주의 사항

*   **Vercel CORS 설정**: `application-prod.yml`의 `allowed-origins`에 반드시 Vercel 도메인을 추가해야 합니다.
*   **WebSocket**: Nginx 설정에서 `Upgrade`, `Connection` 헤더가 제대로 전달되는지 확인하십시오. (`nginx/conf.d/app.conf` 참고)
*   **보안**: DB 패스워드나 Secret 키는 절대 GitHub에 직접 올리지 말고 Environment Variable이나 GitHub Secrets를 사용하십시오.

---
*문서 작성일: 2026-06-18*
