# 🛡️ DeepScan

> **Reality Defender API와 IMD 분석 모델을 활용한 AI 기반 딥페이크 이미지 탐지 및 검증 이력 관리 서비스**

이미지 업로드만으로 딥페이크 의심 여부를 분석하고, 의심 영역 시각화와 검증 이력을 함께 제공하도록 개발한 개인 프로젝트입니다.

🔗 배포 URL 입력

---

## 📌 Overview

DeepScan은 사용자가 이미지의 딥페이크 가능성을 쉽게 확인할 수 있도록 만든 웹 서비스입니다.

* 딥페이크 이미지 분석
* 의심 영역 시각화
* 분석 결과 및 검증 이력 관리
* 딥페이크 관련 뉴스 조회
* 커뮤니티 기반 정보 공유

**Development Period**

* 2026.03 ~ 2026.06

---

## ⚙️ Tech Stack

| Category       | Technology                                      |
| -------------- | ----------------------------------------------- |
| Backend        | Java 17, Spring Boot 3.3.12, Spring MVC         |
| View           | JSP, JSTL, HTML/CSS, JavaScript                 |
| ORM / SQL      | MyBatis                                         |
| Database       | MariaDB, H2, MongoDB                            |
| Infrastructure | AWS EC2, AWS RDS, AWS S3, Docker                |
| External API   | Reality Defender API, Gemini API, Naver News API, Google OAuth |
| AI Service     | IMD, FastAPI, Uvicorn                           |

---

## 🚀 Main Features

### 🔐 Authentication

* 회원가입 / 로그인
* 세션 기반 사용자 인증
* 이메일 인증 코드 발송
* 아이디 찾기 / 비밀번호 재설정
* Google OAuth 로그인
* 프로필 수정 및 회원 탈퇴
* 비밀번호 암호화 처리

### 🖼 Deepfake Detection

* 이미지 업로드 기반 딥페이크 분석
* Reality Defender API 연동
* IMD 모델 기반 히트맵 생성
* 이미지 MIME 타입 및 용량 검증
* 분석 불가 이미지 사전 필터링
* 분석 결과 점수 및 판정 제공

### 🔎 Verification Result

* 분석 결과 상세 페이지
* REAL / SUSPICIOUS / FAKE / NOT_APPLICABLE 상태 제공
* Gemini 기반 GPT 상세 해설 생성
* 의심 영역 좌표 및 confidence 표시
* 분석 원본 JSON 저장
* 사용자별 검증 이력 조회
* 분석 결과 삭제

### 📰 News Service

* Naver News API 기반 딥페이크 뉴스 조회
* 키워드 검색 및 정렬
* MongoDB 기반 뉴스 캐싱
* 중복 기사 제거
* 기사 상세 요약 제공
* 딥페이크 관련 기사 필터링

### 💬 Community

* 게시글 작성 / 수정 / 삭제
* 댓글 및 대댓글 작성
* 게시글 좋아요
* 댓글 좋아요
* 주제별 커뮤니티 게시판
* 조회수 관리

---

## 🏗 Architecture

```text
Browser
   │
 HTTP / HTTPS
   │
Spring Boot
   ├── Controller
   ├── Service
   ├── Mapper
   ├── DTO
   │
   ├── MariaDB
   │     ├── User
   │     ├── Verification
   │     ├── Community
   │     └── Email Auth
   │
   ├── MongoDB
   │     └── News Cache
   │
   ├── Object Storage
   │     ├── Local Uploads
   │     └── AWS S3
   │
   └── External Services
         ├── Reality Defender API
         ├── Gemini API
         ├── IMD FastAPI Service
         ├── Naver News API
         └── Google OAuth
```

---

## 🔑 Key Implementations

### Reality Defender + IMD Analysis Flow

* 이미지 업로드 후 서버 측 파일 검증
* Reality Defender API를 통한 딥페이크 분석
* IMD 서비스로 히트맵 시각화 데이터 생성
* API 응답과 히트맵 결과 병합
* 분석 결과를 DB와 JSON 스냅샷으로 저장

### Image Preflight Validation

* 이미지 파일 여부 확인
* 파일 크기 제한
* 얼굴 영역이 부족한 이미지 필터링
* 분석 불가 결과를 별도 상태로 관리

### News Cache System

* Naver News API 호출
* MongoDB 캐시 저장
* 로컬 메모리 캐시 적용
* 제목 유사도 기반 중복 기사 제거
* API 호출량 감소 및 응답 속도 개선

### Object Storage

* 로컬 업로드 저장소 지원
* AWS S3 저장소 지원
* 환경 설정에 따라 저장 방식 전환
* 업로드 파일 public URL 관리

### Gemini AI Explanation

* Reality Defender 분석 결과를 기반으로 사용자용 상세 해설 생성
* 기존 분석 설명을 반복하지 않고 점수, 한계, 확인 절차를 보강
* 입력 데이터 1,800자 제한 및 Base64 / 히트맵 / 중복 원문 제거
* 출력 토큰 450개 제한, 30분 메모리 캐시, 동시 중복 호출 방지
* 생성된 해설을 DB에 저장하고 결과 화면 재진입 시 자동 복원
* 로컬 실행 시 `.env.properties`에 `GEMINI_API_KEY=발급받은_키`를 설정

---

## 📂 Project Structure

```text
src/main/java/kopo/poly
├── config
├── controller
│   └── api
├── document
├── dto
├── entity
├── mapper
├── repository
├── service
│   └── impl
└── util

src/main/resources
├── mapper
└── application.properties

src/main/webapp
├── WEB-INF/views
└── resources

imd-service
├── app.py
├── Dockerfile
└── requirements.txt
```

---

## 🛠 Trouble Shooting

| Issue                    | Solution                                      |
| ------------------------ | --------------------------------------------- |
| 외부 API 응답 실패        | mock mode 및 예외 처리로 분석 흐름 유지 |
| 분석 불가 이미지 처리     | 얼굴 영역 / 이미지 크기 사전 검증 로직 추가 |
| 뉴스 API 중복 기사 문제   | 제목 정규화 및 유사도 기반 중복 제거 적용 |
| 업로드 파일 관리 문제     | Local / S3 저장소 전략 분리 |
| JSP null 데이터 오류      | 분석 결과 기본값 보정 및 JSON 스냅샷 저장 |
| DB 환경 차이 문제         | H2 기본값과 MariaDB 환경변수 설정 분리 |

---

## 📚 What I Learned

* Spring Boot MVC 기반 웹 서비스 구조
* MyBatis를 활용한 DB 연동
* 외부 AI 분석 API 연동 방식
* FastAPI 기반 Python 분석 서비스 연동
* 이미지 업로드 및 파일 검증 처리
* MongoDB 캐시와 메모리 캐시 활용
* AWS EC2 / RDS / S3 기반 배포 구성
* 사용자 인증, 이메일 인증, OAuth 흐름 구현
* 예외 처리와 장애 상황 fallback 설계

---

## 👨‍💻 Developer

**JUN2973**

Backend Developer

GitHub : https://github.com/JUN2973

Email : wnsdud2973@gmail.com
