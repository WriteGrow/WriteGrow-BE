# 서버 배포

공인 IP 한 대에 Postgres·애플리케이션(컨테이너)과 nginx(호스트)를 함께 올린다.
도메인을 사지 않고 `sslip.io` 로 HTTPS 를 붙인다.

**배포는 GitHub Actions 가 한다.** `main` 에 반영되면 테스트 → 이미지 빌드(GHCR) →
서버 배포까지 자동으로 이어진다(`.github/workflows/deploy.yml`). 서버는 이미지를
pull 만 하므로 소스도 JDK 도 필요 없다.

아래 1~4는 **처음 한 번만** 하는 서버 준비 작업이다.

## 1. 서버 준비

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker
sudo apt-get install -y nginx certbot

# 배포 워크플로가 compose 파일과 .env 를 여기에 쓴다.
sudo mkdir -p /opt/writegrow && sudo chown "$USER" /opt/writegrow
```

## 2. 인증서 발급

`<공인IP>.sslip.io` 가 그대로 호스트명이 된다. `sslip.io` 는 IP 를 호스트명으로 돌려주는
무료 DNS 라 도메인을 사지 않아도 Let's Encrypt 인증서를 발급받을 수 있다.

HTTPS 가 필요한 이유는 보안만이 아니다 — 프론트가 HTTPS 로 배포되면 브라우저가 HTTP API
호출을 혼합 콘텐츠로 차단해서 통신 자체가 되지 않는다.

```bash
export WG_HOST=<공인IP>.sslip.io
sudo systemctl stop nginx
sudo certbot certonly --standalone -d "$WG_HOST"
```

제공사 콘솔에 방화벽이 따로 있다면 **80·443 을 먼저 열어야 한다.** 막혀 있으면
`Timeout during connect` 로 발급에 실패한다.

## 3. nginx 설정

```bash
git clone https://github.com/WriteGrow/WriteGrow-BE.git /tmp/wg && cd /tmp/wg
sed "s/__HOST__/$WG_HOST/g" deploy/nginx/writegrow.conf | sudo tee /etc/nginx/sites-available/writegrow > /dev/null
sudo ln -sf /etc/nginx/sites-available/writegrow /etc/nginx/sites-enabled/writegrow
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl start nginx
```

## 4. GitHub Secrets

리포지토리 → Settings → Secrets and variables → Actions.

| 이름 | 값 |
| :--- | :--- |
| `SSH_HOST` | 서버 공인 IP |
| `SSH_USER` | SSH 접속 계정 (예: `ubuntu`) |
| `SSH_KEY` | 개인키 **전체 내용** (`-----BEGIN` 부터 `-----END` 줄까지) |
| `SSH_PORT` | 기본 22 가 아닐 때만 |
| `POSTGRES_PASSWORD` | DB 비밀번호 (아무 값이나 정해서 넣는다) |
| `S3_BUCKET` | 손글씨 저장 버킷 이름 |
| `S3_ACCESS_KEY` | IAM 사용자 액세스 키 |
| `S3_SECRET_KEY` | IAM 사용자 비밀 키 |

저장소는 AWS S3 지만 서버가 AWS 가 아니라 IAM 역할을 쓸 수 없으므로 키를 직접 주입한다.
그래서 SDK 기본 이름(`AWS_ACCESS_KEY_ID`) 대신 용도가 드러나는 `S3_` 접두사를 쓴다.

### 토글 (Variables 탭)

비밀이 아니라 켜고 끄는 값이므로 **Secrets 가 아니라 Variables** 에 둔다. Secret 으로 두면
값과 같은 문자열(`true`)이 로그에서 전부 마스킹되어 빌드 로그를 읽기 어려워진다.

| 이름 | 기본값 | 언제 바꾸나 |
| :--- | :--- | :--- |
| `AI_STUB` | `true` | AI 서버가 배포되면 `false`. 그 전에 false 로 두면 손글씨 글이 전부 `ANALYSIS_FAILED` 가 된다 |
| `SWAGGER_ENABLED` | `true` | 프론트 연동이 끝나면 `false` |

변수를 만들지 않으면 위 기본값으로 동작한다. 값을 바꾸면 다음 배포부터 적용된다.
`S3_REGION` 과 DB 이름은 바뀔 일이 없어 `deploy.yml` 안에 그대로 적혀 있다.

`S3_BUCKET` 은 dev 와 같은 버킷을 쓴다. `prod/` 접두사로 영역만 나누므로 로컬 개발과
섞이지 않는다. 다른 이름을 넣으면 기동 시 버킷 확인에서 바로 실패한다.

## 5. 배포

`main` 에 머지하면 자동으로 돈다. 수동 실행은 Actions 탭 → Deploy → Run workflow.

배포 워크플로가 마지막에 `/actuator/health` 를 최대 3분간 확인한다. S3 설정이 틀리면
`S3BucketVerifier` 가 기동을 막으므로 여기서 실패로 잡히고, 최근 로그 120줄이 함께 출력된다.

### 확인

```bash
curl -i "https://<공인IP>.sslip.io/actuator/health"   # {"status":"UP"}
curl -i "http://<공인IP>.sslip.io/actuator/health"    # 301 → https
```

프론트에는 `https://<공인IP>.sslip.io` 를 API 주소로, 문서는
`https://<공인IP>.sslip.io/swagger-ui/index.html` 을 알려준다.

Swagger 노출은 `SWAGGER_ENABLED` 로 제어한다(`deploy.yml` 에서 `true` 로 쓴다).
인증이 없으므로 연동이 끝나면 `false` 로 되돌린다.

### 롤백

이미지는 커밋 SHA 로도 태깅된다. 서버에서 `.env` 의 `IMAGE_TAG` 를 이전 SHA 로 바꾸고
다시 올리면 된다.

```bash
cd /opt/writegrow
sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=<이전 커밋 SHA>/' .env
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

## 인증서 갱신

발급은 `--standalone` 으로 했지만 갱신은 nginx 를 내리지 않고 webroot 로 한다.
설정에 `/.well-known/acme-challenge/` 가 열려 있다. 인증서는 90일짜리다.

```bash
sudo certbot certonly --webroot -w /var/www/html -d "$WG_HOST" --force-renewal
sudo systemctl reload nginx
```

## 주의

**인증이 없고 접근 제한도 두지 않았다.** `X-Profile-Id` 헤더 값만 바꾸면 누구나 다른
아동의 글을 읽고 쓸 수 있다. 시연이 끝나면 서버를 내리거나, 아래를 `location /` 안에
넣어 접근을 좁힌다.

```nginx
allow <허용할 IP>;
deny all;
```
