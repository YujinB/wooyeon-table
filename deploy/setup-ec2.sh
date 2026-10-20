#!/usr/bin/env bash
# EC2(Ubuntu 24.04)에서 처음 한 번: Docker·Compose 설치, 메모리가 작아도 버티게 스왑 2GB
set -euo pipefail

CODENAME="$(. /etc/os-release && echo "$VERSION_CODENAME")"
if ! command -v docker >/dev/null 2>&1 \
    && ! curl -fsI "https://download.docker.com/linux/ubuntu/dists/${CODENAME}/stable/Release" >/dev/null; then
  # Docker 공식 저장소에 아직 없는 새 버전(예: 26.04)은 우분투 패키지로 설치
  sudo apt-get update -y
  sudo apt-get install -y docker.io docker-compose-v2
  sudo usermod -aG docker "$USER"
fi

if ! command -v docker >/dev/null 2>&1; then
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${CODENAME} stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
  sudo usermod -aG docker "$USER"
fi

if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

sudo timedatectl set-timezone Asia/Seoul
mkdir -p ~/wooyeon/data
echo "준비 완료. 다시 접속하면 docker 명령을 sudo 없이 쓸 수 있어요."
