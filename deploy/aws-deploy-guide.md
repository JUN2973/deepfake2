<!-- 체크리스트 기준 주석: 배포 스크립트/문서: 운영 배포와 외부 분석 서비스 실행에 필요한 설정을 정리한다. -->
# AWS RDS + EC2 Deployment Guide

This guide follows the class materials:

- `16.1아마존 클라우드 컴퓨팅-RDS활용.pptx`
- `17.1.아마존 클라우드 컴퓨팅-스프링부트배포.pptx`

## 1. RDS MariaDB

Create an RDS MariaDB instance with the free-tier template.

Recommended classroom settings:

- DB identifier: `MariaDB`
- Master user: `admin`
- Port: `3306`
- Public access: `Yes`

Open the RDS security group inbound rule for MariaDB:

- Type: `MYSQL/Aurora`
- Port: `3306`
- Source: your PC IP for local testing, or the EC2 security group for deployment

Create the project database and user from DataGrip or IntelliJ Database console:

```sql
CREATE DATABASE myDB
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

CREATE USER 'poly'@'%' IDENTIFIED BY 'CHANGE_ME_STRONG_DB_PASSWORD';
GRANT ALL PRIVILEGES ON myDB.* TO 'poly'@'%';
FLUSH PRIVILEGES;
```

## 2. Spring Boot Settings

The project already uses:

- Server port: `11000`
- MariaDB driver: `org.mariadb.jdbc.Driver`
- External secret file: `.env.properties`
- Gradle WAR packaging, because this project uses JSP

On EC2, create `/spring_module/.env.properties` from `deploy/aws-env.properties.example` and replace:

- `YOUR_RDS_ENDPOINT`
- `YOUR_EC2_PRIVATE_OR_PUBLIC_IP`
- API keys and OAuth credentials

Example RDS URL:

```properties
DB_URL=jdbc:mariadb://mariadb.xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com:3306/myDB?sslMode=trust&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
DB_USERNAME=poly
DB_PASSWORD=CHANGE_ME_STRONG_DB_PASSWORD
```

## 3. Build

Run this on the development PC:

```powershell
.\gradlew.bat clean test bootWar
```

The deployable file is created under:

```text
build/libs/deepfake2-0.0.1-SNAPSHOT.war
```

## 4. EC2 Folders

Run this on EC2:

```bash
sudo mkdir -p /spring_module
sudo chown -R ec2-user:ec2-user /spring_module
```

Upload these files to `/spring_module`:

- `build/libs/deepfake2-0.0.1-SNAPSHOT.war`
- `.env.properties`

If log or upload directories are needed, create them with `ec2-user` ownership:

```bash
mkdir -p /spring_module/uploads
```

## 5. Run

Run the app in the foreground first to check errors:

```bash
cd /spring_module
java -Djava.net.preferIPv4Stack=true -jar deepfake2-0.0.1-SNAPSHOT.war
```

If it starts correctly, stop it and run in the background:

```bash
nohup java -Djava.net.preferIPv4Stack=true -jar deepfake2-0.0.1-SNAPSHOT.war 1> /dev/null 2>&1 &
```

## 6. EC2 Security Group

Open the EC2 security group inbound rule:

- Type: `Custom TCP`
- Port: `11000`
- Source: your PC IP for testing, or the required public source range

Then open:

```text
http://EC2_PUBLIC_IP:11000/
```

## 7. Common Checks

If the app cannot connect to RDS:

- Confirm RDS inbound `3306` allows the EC2 security group or EC2 IP
- Confirm the DB name is `myDB`
- Confirm `poly` has privileges on `myDB`
- Confirm `/spring_module/.env.properties` is in the same directory where the app is executed

If the app page cannot be opened:

- Confirm EC2 inbound `11000` is open
- Confirm the app is running with `ps -ef | grep deepfake2`
- Confirm no other process is using port `11000`

```bash
netstat -tnlp | grep 11000
```
