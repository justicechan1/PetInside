# 🐾 PetInside (멋쟁이사자처럼 백엔드 24기 — 5팀)

반려동물 집사들을 위한 Q&A · 자랑 커뮤니티 플랫폼 — 궁금한 것을 질문하고 반려동물 일상을 자랑하는 지식 공유 서비스

---

## 📌 주요 기능

### 인증 / 계정
- 일반 회원가입 / 로그인 — 아이디 + 비밀번호 (BCrypt 암호화)
- 소셜 로그인 (OAuth2) — Google OIDC
- JWT 인증 — Access Token(1시간) + Refresh Token(7일)
- 닉네임 변경, 비밀번호 변경, 프로필 이미지 변경
- 로그아웃 (Refresh Token 만료 처리)

### 게시글 (Post)
- Q&A / 자랑(BOAST) 카테고리 구분
- 게시글 작성 / 수정 / 삭제 (작성자 본인만)
- 이미지 다중 첨부 (최대 5장, EC2 로컬 디스크 저장)
- 카테고리 필터 + 키워드 검색 + 페이징 (최신순)
- 조회수 기반 인기 게시글 조회

### 댓글 (Comment)
- 댓글 작성 / 수정 / 삭제
- 대댓글(부모-자식 계층 구조) 지원

### 마이페이지
- 내 정보 조회 (닉네임, 이메일, 프로필 이미지, 가입일)
- 닉네임 변경 (중복 검증)
- 비밀번호 변경 (현재 비밀번호 확인, 일반 계정 전용)
- 프로필 이미지 업로드 및 변경
- 내 게시글 목록 조회 (카테고리 필터, 페이징)

### 관리자
- 회원 목록 조회
- 게시글 / 댓글 강제 삭제
- 회원 권한 변경 (USER ↔ ADMIN)
- 일일 통계 (가입자 수, 게시글 수, 접속자 수)

---

## 🛠 기술 스택

### Backend

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.3 |
| Persistence | Spring Data JPA (Hibernate) |
| Security | Spring Security, OAuth2 Client (Google OIDC), JWT (jjwt 0.12.6) |
| Database | MySQL 8 |
| Docs | springdoc-openapi 2.8.5 (Swagger UI) |
| Build | Gradle |

### Frontend

| 구분 | 기술 |
|------|------|
| Framework | React 19 |
| Build Tool | Vite 8 |
| Language | TypeScript |
| Routing | React Router DOM 7 |
| HTTP | Axios |

### Infra / DevOps

- AWS EC2 (Amazon Linux)
- Nginx 리버스 프록시, Let's Encrypt (Certbot) HTTPS
- GitHub Actions CI/CD — `develop` 브랜치 push 시 자동 배포

---

## 📂 프로젝트 구조

```
PetInside/
├── backend/                          # Spring Boot 애플리케이션
│   └── src/main/java/org/example/petinside/
│       ├── domain/
│       │   ├── admin/                # 관리자 (회원·게시글·통계)
│       │   ├── auth/                 # 회원가입, 로그인, 토큰 관리
│       │   ├── comment/              # 댓글 · 대댓글
│       │   ├── mypage/               # 마이페이지 (닉네임·비밀번호·이미지·내글)
│       │   ├── post/                 # 게시글 (QNA/BOAST, 이미지)
│       │   └── user/                 # 유저 엔티티, 소셜 계정
│       ├── global/
│       │   ├── exception/            # 전역 예외 처리
│       │   ├── response/             # 공통 응답 형식
│       │   ├── security/             # JWT 필터, OAuth2 핸들러
│       │   └── upload/               # 이미지 업로드 (EC2 로컬 디스크)
│       └── config/                   # Security, Swagger, JPA Auditing
│
└── frontend/                         # React + TypeScript
    └── src/
        ├── api/                      # Axios API 모듈 (auth, post, mypage, image, admin)
        ├── components/               # GNB 등 공통 컴포넌트
        ├── pages/                    # 페이지 컴포넌트
        │   ├── MainPage.tsx          # 메인 (인기 Q&A / 화제 자랑글)
        │   ├── AuthPage.tsx          # 로그인 / 회원가입
        │   ├── PostListPage.tsx      # 게시글 목록
        │   ├── PostDetailPage.tsx    # 게시글 상세 + 댓글
        │   ├── PostFormPage.tsx      # 게시글 작성 / 수정
        │   ├── MyPage.tsx            # 마이페이지
        │   ├── AdminPage.tsx         # 관리자
        │   └── OAuthCallbackPage.tsx # 소셜 로그인 콜백
        └── utils/                    # 인증 유틸
```

---

## 🔌 API 개요

Base URL: `/api/v1`

### Auth (`/api/v1/auth`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/signup` | 회원가입 | ✕ |
| POST | `/login` | 로그인 (AccessToken 반환) | ✕ |
| POST | `/refresh` | Access Token 재발급 | Bearer |
| POST | `/logout` | 로그아웃 | Bearer |
| POST | `/oauth/exchange` | OAuth2 코드 → JWT 교환 | ✕ |

### 마이페이지 (`/api/v1/mypage`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/me` | 내 프로필 조회 |
| PATCH | `/me/nickname` | 닉네임 변경 |
| PATCH | `/me/password` | 비밀번호 변경 |
| PATCH | `/me/profile-image` | 프로필 이미지 변경 |
| GET | `/me/posts` | 내 게시글 목록 |

### 게시글 (`/api/v1/posts`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 목록 조회 / 검색 (category, keyword, sort, 페이징) | ✕ |
| GET | `/{postId}` | 상세 조회 | ✕ |
| POST | `/` | 게시글 작성 | ○ |
| PATCH | `/{postId}` | 게시글 수정 | ○ (작성자) |
| DELETE | `/{postId}` | 게시글 삭제 | ○ (작성자 / ADMIN) |

### 댓글 (`/api/v1/posts/{postId}/comments`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/` | 댓글 목록 조회 |
| POST | `/` | 댓글 작성 |
| PATCH | `/{commentId}` | 댓글 수정 |
| DELETE | `/{commentId}` | 댓글 삭제 |

### 이미지 (`/api/v1/images`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/upload` | 이미지 업로드 → URL 반환 |

### 관리자 (`/api/v1/admin`) — ADMIN 권한 전용

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/users` | 회원 목록 조회 |
| PATCH | `/users/{userId}/role` | 권한 변경 |
| DELETE | `/posts/{postId}` | 게시글 강제 삭제 |
| DELETE | `/comments/{commentId}` | 댓글 강제 삭제 |
| GET | `/statistics/daily` | 일일 통계 |

소셜 로그인: `GET /oauth2/authorization/google` → 성공 시 프론트엔드 `/oauth/callback`으로 리다이렉트

전체 API 명세는 Swagger UI에서 확인할 수 있습니다.
- 로컬: `http://localhost:8080/swagger-ui/index.html`
- 배포: `https://15-164-213-118.nip.io/swagger-ui/index.html`

---

## 🗂 데이터 모델 (핵심 엔티티)

| 엔티티 | 설명 | 주요 관계 |
|--------|------|-----------|
| User | 사용자 (username, password, nickname, profileImageUrl, role) | 1:N Post, 1:N Comment |
| SocialAccount | 소셜 계정 연동 정보 | N:1 User |
| Post | 게시글 (title, content, category, viewCount) | N:1 User, 1:N PostImage, 1:N Comment |
| PostImage | 게시글 첨부 이미지 URL | N:1 Post |
| Comment | 댓글 · 대댓글 (대댓글용 self-reference) | N:1 Post, N:1 User, 1:N children |
| RefreshToken | Refresh Token (해시값 저장) | N:1 User |

Role: `USER` (일반) · `ADMIN` (관리자)
Category: `QNA` · `BOAST`

---

## 🚀 시작하기

### 사전 준비

- Java 17
- Node.js 20+
- MySQL 8

### 1. Backend 실행

`backend/.env` 파일 생성 후 실행

```env
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_256bit_or_longer
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
CORS_ALLOWED_ORIGINS=http://localhost:5173
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth/callback
```

```bash
cd backend
./gradlew bootRun
```

서버는 `8080` 포트에서 실행됩니다.

### 2. Frontend 실행

```bash
cd frontend
npm install
```

`.env` 파일 생성:

```env
VITE_API_URL=http://localhost:8080
```

```bash
npm run dev
```

프론트엔드는 `5173` 포트에서 실행됩니다.

---

## ⚙️ 주요 환경변수

| 변수 | 설명 |
|------|------|
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 연결 정보 |
| `JWT_SECRET` | JWT 서명 시크릿 (256비트 이상) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 자격증명 |
| `GOOGLE_REDIRECT_URI` | Google 리디렉션 URI |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 출처 |
| `OAUTH2_REDIRECT_URI` | OAuth2 성공 후 프론트엔드 콜백 URI |
| `UPLOAD_DIR` | 이미지 업로드 디렉터리 (기본: `./uploads`) |
| `VITE_API_URL` | (Frontend) 백엔드 API 주소 |

---

## 🔒 보안 설계

- **인증 방식**: 무상태(Stateless) JWT — Access Token은 `Authorization: Bearer` 헤더, Refresh Token은 DB 저장
- **비밀번호**: BCrypt 단방향 암호화
- **CORS**: 허용 Origin 화이트리스트 (localhost, 배포 도메인)
- **접근 제어**: 공개 API(게시글 조회 등)와 인증 필요 API, ADMIN 전용 API 분리
- **전역 예외 처리**: `GlobalExceptionHandler`에서 예외 타입 → HTTP 상태 코드 매핑

---

## 🌐 배포 환경

| 항목 | 내용 |
|------|------|
| 서버 | AWS EC2 (Amazon Linux) |
| 도메인 | https://15-164-213-118.nip.io |
| SSL | Let's Encrypt (Certbot) |
| 프록시 | Nginx (리버스 프록시, 이미지 서빙) |
| CI/CD | GitHub Actions (`develop` 브랜치 push 시 자동 배포) |

### 브랜치 전략

```
feature/* → develop (PR · Squash and merge)
develop   → main    (배포 시 PR)
release/* → 평가용 스냅샷 브랜치
```

---

## 👥 팀

멋쟁이사자처럼 백엔드 24기 — 5팀 (PetInside)

`main` 브랜치를 참조하여 확인하시면 됩니다.
