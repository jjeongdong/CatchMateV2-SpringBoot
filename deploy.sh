#!/bin/bash

# Blue/Green 배포 스크립트

IS_BLUE=$(docker ps | grep catchmate-blue)

if [ -z "$IS_BLUE" ]; then
    echo "### BLUE 배포 시작 ###"
    docker-compose pull catchmate-blue
    docker-compose up -d catchmate-blue

    while [ 1 = 1 ]; do
        echo "### BLUE 헬스체크 중... ###"
        sleep 3
        REQUEST=$(curl -s http://localhost:8081/actuator/health)
        if [ -n "$REQUEST" ]; then
            if [ "$(echo $REQUEST | grep 'UP')" ]; then
                echo "### BLUE 성공 ###"
                break
            fi
        fi
    done

    echo "### NGINX 스위칭 ###"
    echo "set \$service_url http://catchmate-blue:8080;" | sudo tee ./nginx/conf.d/service-env.inc
    docker-compose exec -T nginx nginx -s reload

    echo "### GREEN 중단 ###"
    docker-compose stop catchmate-green
else
    echo "### GREEN 배포 시작 ###"
    docker-compose pull catchmate-green
    docker-compose up -d catchmate-green

    while [ 1 = 1 ]; do
        echo "### GREEN 헬스체크 중... ###"
        sleep 3
        REQUEST=$(curl -s http://localhost:8082/actuator/health)
        if [ -n "$REQUEST" ]; then
            if [ "$(echo $REQUEST | grep 'UP')" ]; then
                echo "### GREEN 성공 ###"
                break
            fi
        fi
    done

    echo "### NGINX 스위칭 ###"
    echo "set \$service_url http://catchmate-green:8080;" | sudo tee ./nginx/conf.d/service-env.inc
    docker-compose exec -T nginx nginx -s reload

    echo "### BLUE 중단 ###"
    docker-compose stop catchmate-blue
fi
