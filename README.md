# 🐾 PetInside (멋쟁이사자처럼 백엔드 24기 — 3팀)

반려동물 집사들을 위한 Q&A · 자랑 커뮤니티 플랫폼 — 궁금한 것을 질문하고 반려동물 일상을 자랑하는 지식 공유 서비스

🌐 **배포 주소**: https://15-164-213-118.nip.io  
📄 **Swagger UI**: https://15-164-213-118.nip.io/swagger-ui/index.html

---

## 📌 주요 기능

### 인증 / 계정
- 일반 회원가입 / 로그인 — 아이디 + 비밀번호 (BCrypt 암호화)
- 소셜 로그인 (OAuth2) — Google OIDC
- JWT 인증 — Access Token(1시간) + Refresh Token(7일, HttpOnly 쿠키)
- 닉네임 변경, 비밀번호 변경, 프로필 이미지 변경
- 로그아웃 (Refresh Token 만료 처리)

### 게시글 (Post)
- Q&A / 자랑(BOAST) 카테고리 구분
- 게시글 작성 / 수정 / 삭제 (작성자 본인만)
- 이미지 다중 첨부 (최대 5장, EC2 로컬 디스크 저장)
- 카테고리 필터 + 키워드 검색 + 페이징 (최신순)
- 조회수 기반 인기 게시글 조회
- 비회원도 게시글 목록 / 상세 조회 가능

### 댓글 (Comment)
- 댓글 작성 / 수정 / 삭제
- 대댓글(부모-자식 계층 구조) 지원

### 좋아요 (Like)
- 게시글 좋아요 / 취소
- 댓글 좋아요 / 취소
- 펫 사진 좋아요 / 취소

### 이모지 (Emoji)
- 게시글 본문에 커스텀 이모지 삽입
- 이모지 목록 조회 및 본문 렌더링

### 반려동물 프로필 (Pet) — 구독자 전용
- 반려동물 등록 / 수정 / 삭제 (이름, 종류, 생일, 소개, 대표 이미지)
- 반려동물 사진 업로드 / 삭제 (캡션 포함)
- 사진 좋아요 / 취소
- 인기 펫 사진 조회 (메인 화면 — 비회원 공개)
- 공개 프로필에서 타 유저의 반려동물 / 사진 조회 (비회원 공개)

### 구독 · 결제 (Subscription / Payment)
- PortOne SDK v2 연동
- 정기결제 (빌링키 발급 → 자동 결제, 월 1,900원)
- 1개월 이용권 (단건 결제)
- 구독 취소 / 상태 조회 (ACTIVE / INACTIVE)
- 결제 내역 조회 (PAID / FAILED 기록, FAILED는 별도 트랜잭션으로 독립 기록)
- PortOne 웹훅 검증

### 마이페이지
- **프로필 탭**: 프로필 이미지 변경, 닉네임 변경, 비밀번호 변경
- **내 게시글 탭**: 카테고리별 게시글 목록, LIST / GRID 레이아웃 토글 (구독자 전용)
- **반려동물 탭**: 펫 프로필 등록 및 사진 관리 (구독자 전용)
- **구독·결제 탭**: 구독 상태 및 결제 내역 확인

### 공개 프로필
- 타 유저의 프로필, 게시글, 반려동물 정보 조회
- 비회원도 조회 가능
- 반려동물 사진 딥링크 지원 (`?tab=pets&petId=&photoId=`)

### 알림 (Notification)
- Web Push API (VAPID) 기반 브라우저 푸시 알림
- 댓글 / 좋아요 발생 시 실시간 알림
- GNB 알림 드롭다운, 알림 클릭 시 해당 게시글로 이동

### 관리자 — ADMIN 권한 전용
- 회원 목록 조회 / 권한 변경 (USER ↔ ADMIN)
- 게시글 / 댓글 강제 삭제
- 구독 / 결제 내역 조회
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
| 결제 | PortOne API v2 |
| 알림 | Web Push (VAPID, java-webpush) |
| Docs | springdoc-openapi 2.8.5 (Swagger UI) |
| Build | Gradle |

### Frontend

| 구분 | 기술 |
|------|------|
| Framework | React 19 |
| Build Tool | Vite |
| Language | TypeScript |
| Routing | React Router DOM 7 |
| HTTP | Axios |
| 결제 | PortOne Browser SDK v2 |

### Infra / DevOps

- AWS EC2 (Amazon Linux)
- Nginx 리버스 프록시, Let's Encrypt (Certbot) HTTPS
- GitHub Actions CI/CD — `develop` 브랜치 push 시 자동 배포

---

## 📂 프로젝트 구조

```
PetInside/
├── backend/
│   └── src/main/java/org/example/petinside/
│       ├── domain/
│       │   ├── admin/          # 관리자 (회원·게시글·구독·통계)
│       │   ├── auth/           # 회원가입, 로그인, 토큰 관리
│       │   ├── comment/        # 댓글 · 대댓글
│       │   ├── emoji/          # 커스텀 이모지
│       │   ├── like/           # 게시글 · 댓글 · 펫 사진 좋아요
│       │   ├── mypage/         # 마이페이지 (프로필·내 게시글)
│       │   ├── notification/   # Web Push 알림
│       │   ├── payment/        # 결제 (PortOne, 결제 내역)
│       │   ├── pet/            # 반려동물 프로필 · 사진
│       │   ├── post/           # 게시글 (QNA/BOAST, 이미지)
│       │   ├── subscription/   # 구독 (정기·단건)
│       │   └── user/           # 유저 엔티티, 소셜 계정, 공개 프로필
│       ├── global/
│       │   ├── exception/      # 전역 예외 처리
│       │   ├── response/       # 공통 응답 형식
│       │   ├── security/       # JWT 필터, OAuth2 핸들러
│       │   └── upload/         # 이미지 업로드
│       └── config/             # Security, Swagger, JPA Auditing
│
└── frontend/
    └── src/
        ├── api/                # Axios API 모듈
        │   ├── axiosInstance.ts      # 인터셉터, 토큰 자동 갱신
        │   ├── authApi.ts / postApi.ts / commentApi.ts
        │   ├── likeApi.ts / emojiApi.ts / imageApi.ts
        │   ├── petApi.ts / mypageApi.ts
        │   ├── subscriptionApi.ts / notificationApi.ts
        │   └── adminApi.ts
        ├── components/
        │   ├── GNB.tsx               # 글로벌 네비게이션
        │   ├── NotificationDropDown.tsx
        │   ├── CommentSection.tsx / LikeButton.tsx
        │   ├── PetStoryBubbles.tsx / PetProfileHeader.tsx / PetPhotoGrid.tsx
        │   ├── PhotoLightbox.tsx / EmojiPicker.tsx / Avatar.tsx
        │   └── VerifiedBadge.tsx / ProtectedAdminRoute.tsx
        └── pages/
            ├── MainPage.tsx          # 메인 (인기 펫 사진 + 인기 게시글)
            ├── AuthPage.tsx          # 로그인 / 회원가입
            ├── OAuthCallbackPage.tsx # 소셜 로그인 콜백
            ├── PostListPage.tsx      # 게시글 목록
            ├── PostDetailPage.tsx    # 게시글 상세 + 댓글
            ├── PostFormPage.tsx      # 게시글 작성 / 수정
            ├── MyPage.tsx            # 마이페이지 (4탭)
            ├── PublicProfilePage.tsx # 공개 프로필
            ├── SubscriptionPage.tsx  # 구독 · 결제
            ├── SubscriptionRedirectPage.tsx  # 모바일 결제 리다이렉트
            ├── PaymentHistoryPage.tsx
            └── AdminPage.tsx         # 관리자
```

---

## 🔌 API 개요

Base URL: `/api/v1`

### Auth (`/api/v1/auth`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/signup` | 회원가입 | ✕ |
| POST | `/login` | 로그인 | ✕ |
| POST | `/reissue` | Access Token 재발급 | 쿠키 |
| POST | `/logout` | 로그아웃 | Bearer |
| POST | `/oauth2/exchange` | OAuth2 코드 → JWT 교환 | ✕ |

### 유저 · 공개 프로필 (`/api/v1/users`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/me` | 내 정보 조회 | Bearer |
| PATCH | `/me/nickname` | 닉네임 변경 | Bearer |
| PATCH | `/me/password` | 비밀번호 변경 | Bearer |
| PATCH | `/me/profile-image` | 프로필 이미지 변경 | Bearer |
| GET | `/{userId}` | 공개 프로필 조회 | ✕ |
| GET | `/{userId}/posts` | 타 유저 게시글 조회 | ✕ |
| GET | `/{userId}/pets` | 타 유저 반려동물 목록 | ✕ |

### 게시글 (`/api/v1/posts`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 목록 조회 / 검색 (category, keyword, sort, 페이징) | ✕ |
| GET | `/{postId}` | 상세 조회 | ✕ |
| POST | `/` | 게시글 작성 | Bearer |
| PUT | `/{postId}` | 게시글 수정 | Bearer |
| DELETE | `/{postId}` | 게시글 삭제 | Bearer |

### 댓글 (`/api/v1/posts/{postId}/comments`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 댓글 목록 | ✕ |
| POST | `/` | 댓글 작성 | Bearer |
| PUT | `/{commentId}` | 댓글 수정 | Bearer |
| DELETE | `/{commentId}` | 댓글 삭제 | Bearer |

### 좋아요 (`/api/v1`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/posts/{postId}/likes` | 게시글 좋아요 토글 | Bearer |
| GET | `/posts/{postId}/likes/me` | 내 좋아요 여부 | ✕ |
| POST | `/comments/{commentId}/likes` | 댓글 좋아요 토글 | Bearer |
| POST | `/pets/{petId}/photos/{photoId}/likes` | 펫 사진 좋아요 토글 | Bearer |

### 반려동물 (`/api/v1/pets`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/photos/popular` | 인기 펫 사진 (메인) | ✕ |
| POST | `/` | 반려동물 등록 | Bearer (구독자) |
| PUT | `/{petId}` | 반려동물 수정 | Bearer |
| DELETE | `/{petId}` | 반려동물 삭제 | Bearer |
| GET | `/{petId}/photos` | 펫 사진 목록 | ✕ |
| POST | `/{petId}/photos` | 펫 사진 업로드 | Bearer |
| DELETE | `/{petId}/photos/{photoId}` | 펫 사진 삭제 | Bearer |

### 구독 · 결제 (`/api/v1`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/subscription` | 내 구독 상태 | Bearer |
| POST | `/subscription/recurring` | 정기결제 빌링키 등록 | Bearer |
| POST | `/subscription/onetime` | 단건 결제 | Bearer |
| DELETE | `/subscription` | 구독 취소 | Bearer |
| GET | `/payments/history` | 결제 내역 | Bearer |
| POST | `/payments/webhook` | PortOne 웹훅 수신 | ✕ |

### 알림 (`/api/v1/notifications`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 알림 목록 | Bearer |
| POST | `/subscribe` | Web Push 구독 | Bearer |
| PATCH | `/{id}/read` | 알림 읽음 처리 | Bearer |

### 이미지 (`/api/v1/images`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/upload` | 이미지 업로드 → URL 반환 | Bearer |

### 관리자 (`/api/v1/admin`) — ADMIN 전용

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/users` | 회원 목록 |
| PATCH | `/users/{userId}/role` | 권한 변경 |
| DELETE | `/posts/{postId}` | 게시글 강제 삭제 |
| DELETE | `/comments/{commentId}` | 댓글 강제 삭제 |
| GET | `/subscriptions` | 구독 목록 |
| GET | `/payments` | 결제 목록 |
| GET | `/statistics/daily` | 일일 통계 |

---

## 🗂 데이터 모델 (핵심 엔티티)

| 엔티티 | 설명 |
|--------|------|
| User | 사용자 (nickname, email, role, profileImageUrl, profileLayout) |
| SocialAccount | 소셜 계정 연동 (Google) |
| Post | 게시글 (title, content, category, viewCount) |
| PostImage | 게시글 첨부 이미지 |
| Comment | 댓글 · 대댓글 (self-reference) |
| PostLike / CommentLike | 좋아요 |
| Pet | 반려동물 프로필 |
| PetPhoto | 반려동물 사진 |
| PetPhotoLike | 펫 사진 좋아요 |
| Subscription | 구독 (status: ACTIVE / INACTIVE, type: RECURRING / ONE_TIME) |
| Payment | 결제 내역 (status: READY / PAID / FAILED) |
| PushSubscription | Web Push VAPID 구독 정보 |
| RefreshToken | Refresh Token |

Role: `USER` · `ADMIN`  
Category: `QNA` · `BOAST`

---

## 🚀 시작하기

### 사전 준비

- Java 17
- Node.js 20+
- MySQL 8

### 1. Backend 실행

`backend/.env` 파일 생성:

```env
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_256bit_or_longer
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
CORS_ALLOWED_ORIGINS=http://localhost:5173
OAUTH2_REDIRECT_URI=http://localhost:5173/oauth/callback
PORTONE_STORE_ID=your_store_id
PORTONE_CHANNEL_KEY=your_channel_key
PORTONE_CHANNEL_KEY_SUBSCRIPTION=your_subscription_channel_key
PORTONE_API_SECRET=your_api_secret
PORTONE_WEBHOOK_SECRET=your_webhook_secret
VAPID_PUBLIC_KEY=your_vapid_public_key
VAPID_PRIVATE_KEY=your_vapid_private_key
VAPID_SUBJECT=mailto:your@email.com
```

```bash
cd backend
./gradlew bootRun
```

서버는 `8080` 포트에서 실행됩니다.

### 2. Frontend 실행

`frontend/.env` 파일 생성:

```env
VITE_API_URL=http://localhost:8080
VITE_VAPID_PUBLIC_KEY=your_vapid_public_key
```

```bash
cd frontend
npm install
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
| `CORS_ALLOWED_ORIGINS` | CORS 허용 출처 |
| `OAUTH2_REDIRECT_URI` | OAuth2 성공 후 프론트 콜백 URI |
| `PORTONE_STORE_ID` | PortOne 상점 ID |
| `PORTONE_CHANNEL_KEY` | PortOne 단건 결제 채널 키 |
| `PORTONE_CHANNEL_KEY_SUBSCRIPTION` | PortOne 정기결제 채널 키 |
| `PORTONE_API_SECRET` | PortOne API 시크릿 |
| `PORTONE_WEBHOOK_SECRET` | PortOne 웹훅 검증 시크릿 |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | Web Push VAPID 키 쌍 |
| `VAPID_SUBJECT` | Web Push 연락처 (mailto:) |
| `VITE_API_URL` | (Frontend) 백엔드 API 주소 |
| `VITE_VAPID_PUBLIC_KEY` | (Frontend) VAPID 공개 키 |

---

## 🔒 보안 설계

- **인증**: Stateless JWT — Access Token은 `Authorization: Bearer` 헤더, Refresh Token은 HttpOnly 쿠키
- **비밀번호**: BCrypt 단방향 암호화
- **CORS**: 허용 Origin 화이트리스트
- **접근 제어**: 비회원 공개 / 로그인 필요 / 구독자 전용 / ADMIN 전용 4단계 분리
- **결제 검증**: PortOne 웹훅 서명 검증, 서버사이드 금액 검증
- **결제 실패 기록**: `REQUIRES_NEW` 독립 트랜잭션으로 부모 롤백과 무관하게 FAILED 상태 기록

---

## 🌐 배포 환경

| 항목 | 내용 |
|------|------|
| 서버 | AWS EC2 (Amazon Linux) |
| 도메인 | https://15-164-213-118.nip.io |
| SSL | Let's Encrypt (Certbot) |
| 프록시 | Nginx (리버스 프록시, 정적 이미지 서빙) |
| CI/CD | GitHub Actions (`develop` 브랜치 push 시 자동 배포) |

### 브랜치 전략

```
feature/* → develop (PR · Squash and merge)
develop   → main    (배포 시 PR)
release/* → 평가용 스냅샷 브랜치
```

---

## 👥 팀

멋쟁이사자처럼 백엔드 24기 — 3팀 (PetInside)
