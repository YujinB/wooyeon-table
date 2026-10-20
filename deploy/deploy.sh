#!/usr/bin/env bash
# 로컬(Git Bash)에서 실행: 빌드 → EC2로 올리기 → 컨테이너 재시작
# 사용: deploy/deploy.sh <EC2 공인 IP> <pem 키 경로>
#   처음 한 번은 deploy/.env 를 만들어 두고(.env.example 참고), data/applicants.xlsx 가 있어야 가상데이터가 들어간다
set -euo pipefail

HOST="${1:?EC2 공인 IP를 주세요}"
KEY="${2:?pem 키 경로를 주세요}"
REMOTE="ubuntu@${HOST}"
DIR="$(cd "$(dirname "$0")/.." && pwd)"
SSH=(ssh -i "$KEY" -o StrictHostKeyChecking=accept-new "$REMOTE")
SCP=(scp -i "$KEY" -o StrictHostKeyChecking=accept-new)

[ -f "$DIR/deploy/.env" ] || { echo "deploy/.env 가 없어요 (.env.example 참고)"; exit 1; }

echo "[1/4] 백엔드·프론트 빌드"
(cd "$DIR" && ./gradlew bootJar -x test -q)
(cd "$DIR/frontend" && npm run build --silent)

echo "[2/4] 서버 준비 (처음이면 Docker·스왑 설치)"
"${SCP[@]}" "$DIR/deploy/setup-ec2.sh" "$REMOTE:~/setup-ec2.sh"
"${SSH[@]}" "bash ~/setup-ec2.sh"

echo "[3/4] 파일 올리기"
"${SSH[@]}" "mkdir -p ~/wooyeon/data && rm -rf ~/wooyeon/dist"
"${SCP[@]}" "$DIR"/build/libs/wooyeon-table-*-SNAPSHOT.jar "$REMOTE:~/wooyeon/app.jar"
"${SCP[@]}" -r "$DIR/frontend/dist" "$REMOTE:~/wooyeon/dist"
"${SCP[@]}" "$DIR/deploy/docker-compose.prod.yml" "$DIR/deploy/nginx.conf" "$DIR/deploy/.env" "$REMOTE:~/wooyeon/"
if [ -f "$DIR/data/applicants.xlsx" ]; then
  "${SCP[@]}" "$DIR/data/applicants.xlsx" "$REMOTE:~/wooyeon/data/applicants.xlsx"
fi

echo "[4/4] 컨테이너 재시작"
"${SSH[@]}" "cd ~/wooyeon && sudo docker compose -f docker-compose.prod.yml --env-file .env up -d --force-recreate backend nginx mysql && sudo docker compose -f docker-compose.prod.yml ps"

echo "완료: http://${HOST}/app/login (참가자) · http://${HOST}/admin/login (운영자)"
