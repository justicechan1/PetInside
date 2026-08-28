# 🐾 PetInside (멋쟁이사자처럼 백엔드 24기 — 3팀)

반려동물 집사들을 위한 Q&A · 자랑 커뮤니티 플랫폼 — 궁금한 것을 질문하고 반려동물 일상을 자랑하는 지식 공유 서비스

🌐 **배포 주소**: https://15-164-213-118.nip.io  
📄 **Swagger UI**: https://15-164-213-118.nip.io/swagger-ui/index.html

---

## 👥 팀원 및 역할분담

> 상세 역할분담: [MVP1](https://app.notion.com/p/mvp1-ab273873401a839997d68155e779937d) · [MVP2](https://app.notion.com/p/mvp2-3ad73873401a80779433e5b8c86dd8f0)

| 이름 | 역할 | MVP1 담당 | MVP2 담당 |
|------|------|-----------|-----------|
| 김태엽 | 팀장 | 마이페이지 (F-05~F-09), Git 관리 | 마이페이지 커스텀 · 펫피드 (F-32, F-33, F-35, F-36) |
| 정의찬 | 부팀장 | 인증 (F-01~F-04), 명세서 · ERD 설계 | 인증/보안 · 유료구독 (F-19~F-25) |
| 차시환 | 팀원 | 게시판 (F-10~F-14) | 콘텐츠 개선 · 이모지 · 공개 프로필 (F-26~F-28, F-37, F-39) |
| 정선우 | 팀원 | 회원/소셜 로그인 (F-04) | 좋아요 · 인증뱃지 (F-29~F-31, F-38) |
| 정동욱 | 팀원 | 관리자 기능 (F-16~F-20) | 알림 · 관리자 · 신고 확장 (F-34, F-40~F-45) |

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
- 구독 취소 / 재개 / 상태 조회 (ACTIVE / PAST_DUE / EXPIRED)
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

### 신고 처리
- 게시글/댓글/대댓글 신고 처리
- 스팸/광고/욕설/음란물 신고 가능

### 관리자 — ADMIN 권한 전용
- 회원 목록 조회 / 권한 변경 (USER ↔ ADMIN)
- 게시글 / 댓글 강제 삭제
- 구독 / 결제 내역 조회
- 일일 통계 (가입자 수, 게시글 수, 접속자 수)
- 신고 목록 조회
- 신고 내역 삭제/반려

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

### Backend

```
backend/src/main/java/org/example/petinside/
├── domain/
│   ├── admin/          # 관리자 (회원·게시글·구독·통계)
│   ├── auth/           # 회원가입, 로그인, 토큰 관리
│   ├── comment/        # 댓글 · 대댓글
│   ├── emoji/          # 커스텀 이모지
│   ├── like/           # 게시글 · 댓글 · 펫 사진 좋아요
│   ├── mypage/         # 마이페이지 · 공개 프로필
│   ├── notification/   # Web Push 알림
│   ├── payment/        # 결제 (PortOne, 결제 내역)
│   ├── pet/            # 반려동물 프로필 · 사진
│   ├── post/           # 게시글 (QNA/BOAST, 이미지)
│   ├── subscription/   # 구독 (정기·단건, 빌링키, 스케줄러)
│   └── user/           # 유저 엔티티, 소셜 계정
├── global/
│   ├── exception/      # 전역 예외 처리
│   ├── portone/        # PortOne 클라이언트
│   ├── response/       # 공통 응답 형식
│   ├── security/       # JWT 필터, OAuth2 핸들러
│   └── upload/         # 이미지 업로드
└── config/             # Security, Swagger, JPA Auditing
```

### Frontend

```
frontend/src/
├── domains/
│   ├── admin/
│   │   ├── api/        # adminApi.ts
│   │   ├── components/ # ProtectedAdminRoute.tsx
│   │   └── pages/      # AdminPage.tsx
│   ├── auth/
│   │   └── pages/      # AuthPage.tsx, OAuthCallbackPage.tsx
│   ├── mypage/
│   │   ├── api/        # mypageApi.ts
│   │   └── pages/      # MyPage.tsx
│   ├── notification/
│   │   ├── api/        # notificationApi.ts
│   │   └── components/ # NotificationDropDown.tsx
│   ├── pet/
│   │   ├── api/        # petApi.ts
│   │   └── components/ # PetPhotoGrid.tsx, PetProfileHeader.tsx, PetStoryBubbles.tsx, PhotoLightbox.tsx
│   ├── post/
│   │   ├── api/        # postApi.ts, commentApi.ts, emojiApi.ts
│   │   ├── components/ # CommentSection.tsx, EmojiPicker.tsx, EmojiText.tsx
│   │   └── pages/      # PostListPage.tsx, PostDetailPage.tsx, PostFormPage.tsx
│   ├── subscription/
│   │   ├── api/        # subscriptionApi.ts
│   │   └── pages/      # SubscriptionPage.tsx, SubscriptionRedirectPage.tsx, PaymentHistoryPage.tsx
│   └── user/
│       └── pages/      # PublicProfilePage.tsx
├── shared/
│   ├── api/            # axiosInstance.ts, imageApi.ts, likeApi.ts
│   ├── components/     # GNB.tsx, Avatar.tsx, VerifiedBadge.tsx, LikeButton.tsx
│   ├── hooks/          # useLike.ts
│   ├── styles/         # common.ts
│   └── utils/          # auth.ts, push.ts, emojiText.ts, profileNav.ts
├── pages/
│   └── MainPage.tsx
├── App.tsx
└── main.tsx
```

---

## 🔌 API

Swagger UI에서 전체 API 명세를 확인할 수 있습니다.

📄 **https://15-164-213-118.nip.io/swagger-ui/index.html**

| 도메인 | Base Path |
|--------|-----------|
| 인증 | `/api/v1/auth` |
| 유저 · 공개 프로필 | `/api/v1/users` |
| 게시글 | `/api/v1/posts` |
| 댓글 | `/api/v1/posts/{postId}/comments` |
| 좋아요 | `/api/v1/posts, /api/v1/comments, /api/v1/pets` |
| 반려동물 | `/api/v1/pets` |
| 빌링키 | `/api/v1/billing-keys` |
| 구독 | `/api/v1/subscriptions` |
| 결제 | `/api/v1/payments` |
| 알림 | `/api/v1/notifications` |
| 이모지 | `/api/v1/emojis` |
| 이미지 | `/api/v1/images` |
| 관리자 | `/api/v1/admin` |

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
```

---

## 📁 프로젝트 문서

| 문서 | 설명 |
|------|------|
| [요구사항](docs/요구사항.md) | MVP1 · MVP2 기능 범위 |
| [사용자흐름](docs/사용자흐름.md) | 로그인 · 게시글 · 구독 플로우차트 |
| [기능명세](docs/기능명세.md) | F-01 ~ F-42 기능 명세 |
| [ERD](docs/ERD.md) | 엔티티 관계도 |
| [API 명세](docs/API.md) | API 요약 + Swagger UI 링크 |
| [화면설계](docs/화면설계.md) | 화면 목록 및 화면↔API 매핑 |
| [패키지구조](docs/패키지구조.md) | Backend · Frontend 폴더 트리 |
| [권한매트릭스](docs/권한매트릭스.md) | GUEST / USER / SUBSCRIBER / ADMIN 권한 분리 |
| [구독상태전이](docs/구독상태전이.md) | ACTIVE → PAST_DUE → EXPIRED 상태 머신 |
| [외부API연동](docs/외부API연동.md) | PortOne V2 · VAPID Web Push · Google OAuth2 |
| [배포가이드](docs/배포가이드.md) | GitHub Actions CI/CD · EC2 구성 |
| [시퀀스](docs/시퀀스.md) | 주요 흐름 시퀀스 다이어그램 |
| [테스트체크리스트](docs/테스트체크리스트.md) | 핵심 시나리오 체크리스트 |
| [트러블슈팅](docs/트러블슈팅.md) | 주요 기술적 이슈와 해결 과정 |
