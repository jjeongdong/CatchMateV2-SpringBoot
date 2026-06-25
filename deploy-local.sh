#!/bin/bash

# =============================================================
# CatchMate 직접(수동) 배포 스크립트 — CI/CD · 무중단(Blue/Green) 미사용
#
# Docker Hub pull 없이 EC2 서버에서 소스를 직접 빌드하고,
# 단일 app 컨테이너(catchmate-app)를 재기동한다.
# (재기동되는 수 초간 짧은 다운타임이 발생할 수 있음)
#
# 사용:  ./deploy-local.sh
# 사전:  - 배포할 소스를 서버에 먼저 반영(git pull 등)해 둘 것
#        - src/main/resources/application-dev.yml, firebase-adminsdk.json 준비
# =============================================================

set -e

# compose 의 image 태그(${DOCKER_USERNAME:-local}/catchmate:latest)와 맞춘다.
export DOCKER_USERNAME=${DOCKER_USERNAME:-local}

echo "### 1. 서버에서 직접 빌드 후 재기동 (app + redis + nginx) ###"
# --build: Dockerfile 로 catchmate-app 이미지를 서버에서 빌드
# 새 이미지면 자동으로 컨테이너가 재생성된다.
docker-compose up -d --build

echo "### 2. 미사용 이미지 정리 ###"
docker image prune -f || true

echo "### 배포 완료 ###"
echo "  상태 확인:  docker ps"
echo "  로그 확인:  docker logs -f catchmate-app"
