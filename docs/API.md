# API 명세

## 원본
- Notion (초안): https://app.notion.com/p/API-3ae73873401a80749e52f1c963af6a07
- Swagger UI (최종): https://15-164-213-118.nip.io/swagger-ui/index.html

> 구현 완료 후 최종 명세는 Swagger UI 기준입니다.

## API 요약

Base URL: `/api/v1`

| 도메인 | Method | Endpoint | 인증 | 설명 |
|--------|--------|----------|------|------|
| **Auth** | POST | `/auth/signup` | ✕ | 회원가입 |
| | POST | `/auth/login` | ✕ | 로그인 |
| | POST | `/auth/reissue` | 쿠키 | Access Token 재발급 |
| | POST | `/auth/logout` | Bearer | 로그아웃 |
| | POST | `/auth/oauth2/exchange` | ✕ | OAuth2 code → JWT |
| **User** | GET | `/users/me` | Bearer | 내 정보 조회 |
| | PATCH | `/users/me/nickname` | Bearer | 닉네임 변경 |
| | PATCH | `/users/me/password` | Bearer | 비밀번호 변경 |
| | PATCH | `/users/me/profile-image` | Bearer | 프로필 이미지 변경 |
| | PATCH | `/users/me/profile-layout` | Bearer | 레이아웃 설정 |
| | GET | `/users/me/posts` | Bearer | 내 게시글 목록 |
| | GET | `/users/{id}` | ✕ | 공개 프로필 조회 |
| | GET | `/users/{id}/posts` | ✕ | 타 유저 게시글 |
| | GET | `/users/{id}/pets` | ✕ | 타 유저 반려동물 |
| **Post** | GET | `/posts` | ✕ | 게시글 목록·검색 |
| | GET | `/posts/{id}` | ✕ | 게시글 상세 |
| | POST | `/posts` | Bearer | 게시글 작성 |
| | PUT | `/posts/{id}` | Bearer | 게시글 수정 |
| | DELETE | `/posts/{id}` | Bearer | 게시글 삭제 |
| **Comment** | GET | `/posts/{id}/comments` | ✕ | 댓글 목록 |
| | POST | `/posts/{id}/comments` | Bearer | 댓글 작성 |
| | PUT | `/comments/{id}` | Bearer | 댓글 수정 |
| | DELETE | `/comments/{id}` | Bearer | 댓글 삭제 |
| **Like** | POST | `/posts/{id}/likes` | Bearer | 게시글 좋아요 토글 |
| | POST | `/comments/{id}/likes` | Bearer | 댓글 좋아요 토글 |
| | POST | `/pets/{petId}/photos/{photoId}/likes` | Bearer | 펫 사진 좋아요 |
| **Pet** | GET | `/pets/photos/popular` | ✕ | 인기 펫 사진 |
| | POST | `/pets` | Bearer(구독) | 반려동물 등록 |
| | GET | `/pets/{id}/photos` | ✕ | 펫 사진 목록 |
| | POST | `/pets/{id}/photos` | Bearer | 펫 사진 업로드 |
| **Subscription** | GET | `/subscription` | Bearer | 구독 상태 |
| | POST | `/subscription/recurring` | Bearer | 정기결제 |
| | POST | `/subscription/onetime` | Bearer | 단건 결제 |
| | DELETE | `/subscription` | Bearer | 구독 취소 |
| **Payment** | GET | `/payments/history` | Bearer | 결제 내역 |
| | POST | `/payments/webhook` | ✕ | PortOne 웹훅 |
| **Notification** | GET | `/notifications` | Bearer | 알림 목록 |
| | POST | `/notifications/subscribe` | Bearer | Push 구독 |
| **Image** | POST | `/images/upload` | Bearer | 이미지 업로드 |
| **Admin** | GET | `/admin/users` | ADMIN | 회원 목록 |
| | PATCH | `/admin/users/{id}/role` | ADMIN | 권한 변경 |
| | DELETE | `/admin/posts/{id}` | ADMIN | 게시글 강제 삭제 |
| | GET | `/admin/statistics/daily` | ADMIN | 일일 통계 |
