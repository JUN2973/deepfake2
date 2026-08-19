<!-- 체크리스트 기준 주석: 배포 스크립트/문서: 운영 배포와 외부 분석 서비스 실행에 필요한 설정을 정리한다. -->
# IMD Docker + GHCR Deployment

This follows the class PPT flow for this project's FastAPI IMD service.

## 1. Build Locally

Run from the project root on your PC:

```powershell
docker compose -f docker-compose.imd.yml build
```

Local test:

```powershell
docker compose -f docker-compose.imd.yml up -d
```

Open:

```text
http://127.0.0.1:8000/health
```

If port `8000` is already used on your PC:

```powershell
$env:IMD_HOST_PORT="18080"
docker compose -f docker-compose.imd.yml up -d
```

Then open:

```text
http://127.0.0.1:18080/health
```

Stop local test:

```powershell
docker compose -f docker-compose.imd.yml down
```

## 2. Push to GHCR

Login:

```powershell
docker login ghcr.io -u YOUR_GITHUB_USERNAME
```

Tag and push:

```powershell
docker tag deepfake2-imd ghcr.io/YOUR_GITHUB_USERNAME/deepfake2-imd:latest
docker push ghcr.io/YOUR_GITHUB_USERNAME/deepfake2-imd:latest
```

## 3. Run on EC2

Install and start Docker if needed:

```bash
sudo yum install -y docker
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
```

After `usermod`, reconnect SSH. Then login:

```bash
docker login ghcr.io -u YOUR_GITHUB_USERNAME
```

Create `docker-compose.imd.prod.yml` on EC2, or copy this repository's `deploy/docker-compose.imd.prod.yml`.
Then set your image and start it:

```bash
export IMD_IMAGE=ghcr.io/YOUR_GITHUB_USERNAME/deepfake2-imd:latest
docker compose -f docker-compose.imd.prod.yml up -d
```

Check:

```bash
curl http://127.0.0.1:8000/health
docker logs -f deepfake2-imd
```

## 4. Spring Boot Settings

In `/spring_module/.env.properties`:

```properties
IMD_BASE_URL=http://127.0.0.1:8000
IMD_HEATMAP_ENABLED=true
IMD_AUTO_START_ENABLED=false
DEEPFAKE_CLIENT_MODE=real
```

Do not open `8000` in the EC2 security group. Only Spring Boot's public port, usually `11000`, needs to be open.

## 5. Update Later

After pushing a new image:

```bash
export IMD_IMAGE=ghcr.io/YOUR_GITHUB_USERNAME/deepfake2-imd:latest
docker pull ghcr.io/YOUR_GITHUB_USERNAME/deepfake2-imd:latest
docker compose -f docker-compose.imd.prod.yml up -d
```
