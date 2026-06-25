# CatchMate AWS Deployment Guide

이 문서는 CatchMate 백엔드 시스템을 AWS(Amazon Web Services) 환경에 배포하기 위한 상세 가이드를 제공합니다. 현재 프로젝트의 Docker 및 Nginx 설정을 기반으로 실무적인 배포 절차를 설명합니다.

---

## 1. 아키텍처 개요

*   **Compute**: AWS EC2 (t3.medium 권장 - 빌드 및 실행 메모리 확보)
*   **Database**: AWS RDS (MySQL 8.0)
*   **Cache/PubSub**: Redis (Docker Compose로 실행)
*   **Storage**: AWS S3 (프로필 이미지 및 파일 업로드)
*   **Reverse Proxy**: Nginx (Docker Compose로 실행)
*   **배포 방식**: EC2 서버에서 소스 직접 빌드 후 단일 컨테이너 재기동 (`deploy-local.sh`)
    *   > ℹ️ GitHub Actions 기반 CI/CD(`.github/workflows/deploy.yml`)와 Blue/Green 무중단 스크립트(`deploy.sh`)도 존재하지만, **현재는 둘 다 사용하지 않고 아래 "직접 배포" 절차만 사용**합니다. (재기동 시 수 초 다운타임 감수)

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

## 4. 직접(수동) 배포 — CI/CD · 무중단 미사용 ⭐

GitHub Actions / Docker Hub 없이, **EC2 서버에서 소스를 직접 빌드**해 배포합니다.
무중단(Blue/Green) 전환도 하지 않고 **단일 app 컨테이너(`catchmate-app`)를 재기동**합니다.
(재기동되는 수 초간 짧은 다운타임 발생 — 일단 단순 배포 우선)

`docker-compose.yml` 은 **단일 인스턴스 기준**(`catchmate-app` + `redis` + `nginx`)이며,
app 컨테이너는 `mem_limit: 1g` 으로 메모리가 제한됩니다.

### 4.1 최초 1회 준비 (EC2)

```bash
# 1) 코드 클론 (배포 폴더는 deploy.sh 의 경로와 동일하게)
cd /home/ubuntu
git clone https://github.com/jjeongdong/CatchMateV2-SpringBoot catchmatev2-springboot
cd catchmatev2-springboot

# 2) 운영 설정 파일 생성 (git 에 올라가지 않는 파일들 — 직접 작성/업로드)
#    - 컨테이너는 SPRING_PROFILES_ACTIVE=dev 로 뜨므로 application-dev.yml 이 필요
vim src/main/resources/application-dev.yml          # DB/Redis/JWT 등 운영 값
#    - Firebase Admin SDK 키 (FCM)
vim src/main/resources/firebase-adminsdk.json       # 또는 scp 로 업로드
```

> ⚠️ `application-dev.yml`, `application-local.yml`, `firebase-adminsdk.json` 은
> `.gitignore` 에 포함되어 있어 clone 시 받아지지 않습니다. 서버에 직접 넣어야 합니다.

### 4.2 배포 실행 (매 배포마다)

```bash
cd /home/ubuntu/catchmatev2-springboot
git pull origin main          # 최신 소스 반영 (스크립트가 자동으로 하지 않음)
./deploy-local.sh
```

`deploy-local.sh` 가 수행하는 일:
1. `docker-compose up -d --build` — **서버에서 직접** app 이미지 빌드 후
   `catchmate-app` 재기동(+`redis`, `nginx` 기동). Docker Hub pull 안 함.
2. 미사용 이미지 정리 (`docker image prune -f`)

> ⚠️ 스크립트는 더 이상 `git pull` 을 하지 않습니다. 배포 전 소스를 직접 최신화하세요.

> Nginx 는 `nginx/conf.d/service-env.inc` 가 `catchmate-app` 을 가리킵니다.

### 4.3 (선택) 무중단 · CI/CD 로 확장하려면
- **무중단(Blue/Green)**: `deploy.sh` 는 `catchmate-blue`/`catchmate-green` 두 컨테이너를 전제로 하므로,
  현재 단일 인스턴스 `docker-compose.yml` 에서는 그대로 동작하지 않습니다. 무중단이 필요하면
  compose 에 blue/green 두 서비스를 복원한 뒤 `deploy.sh` 를 사용하세요.
- **CI/CD**: `.github/workflows/deploy.yml` 이 그대로 남아 있습니다. `main` 푸시 시 동작하도록
  하려면 GitHub Secrets(`DOCKER_USERNAME`, `DOCKER_PASSWORD`, `SERVER_HOST`,
  `SERVER_USER`, `SERVER_SSH_KEY`, `APPLICATION_YML`, `FIREBASE_SERVICE_KEY`)를 설정하면 됩니다.

---

## 5. 부가 설정

### 5.1 SSL 인증서 설정 (Certbot)
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
