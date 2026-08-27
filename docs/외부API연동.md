# 외부 API 연동

## PortOne V2 (결제·구독)

### 연동 목적
- 정기결제(자동갱신) 및 단건(1개월 이용권) 구독 결제 처리
- PortOne SDK V2 사용

### 결제 플로우

#### 정기결제 (자동갱신)
```
1. POST /billing-keys/prepare
   └─ BillingKeyService.prepare()
      └─ PortOne 빌링키 발급 의도 생성 → BillingKeyIssuanceIntent 저장
2. 프론트: PortOne SDK로 카드 등록 → billing_key 수신
3. POST /billing-keys
   └─ BillingKeyService.create()
      └─ PortOne API 검증 → BillingKey 암호화 저장 (AES-256)
4. POST /subscriptions
   └─ SubscriptionService.create()
      └─ 1회차 즉시 결제 → Subscription.activate() → status=ACTIVE
```

#### 단건 결제 (1개월 이용권)
```
1. POST /subscriptions/one-time/prepare
   └─ SubscriptionService.prepareOneTime() → paymentId 생성
2. 프론트: PortOne SDK로 결제 진행
3. POST /subscriptions/one-time/complete
   └─ SubscriptionService.completeOneTime()
      └─ PortOne 재조회 → 금액·상태 검증
      └─ Subscription.purchaseOneTime() → status=ACTIVE (canceledAt 즉시 세팅)
```

#### 웹훅 처리
- 엔드포인트: `POST /api/v1/payments/webhook`
- 검증: `PORTONE_WEBHOOK_SECRET`으로 HMAC 서명 확인
- 처리 이벤트: `Transaction.Paid` (결제 완료), `Transaction.Cancelled` (환불 → 즉시 EXPIRED), `BillingKey.Issued`
- 실패 기록: `@Transactional(propagation = Propagation.REQUIRES_NEW)` — 웹훅 처리 실패 시에도 `PaymentTransaction` 기록 보존

### 환경변수

| 변수명 | 설명 |
|--------|------|
| `PORTONE_STORE_ID` | PortOne 상점 ID |
| `PORTONE_CHANNEL_KEY` | 단건 결제 채널 키 |
| `PORTONE_CHANNEL_KEY_SUBSCRIPTION` | 정기결제 채널 키 |
| `PORTONE_API_SECRET` | PortOne REST API 인증 시크릿 |
| `PORTONE_WEBHOOK_SECRET` | 웹훅 서명 검증 키 |
| `PORTONE_WEBHOOK_NOTICE_URL` | 웹훅 수신 URL (`https://15-164-213-118.nip.io/api/v1/payments/webhook`) |
| `BILLING_KEY_ENCRYPTION_SECRET` | 빌링키 AES-256 암호화 키 |

---

## Web Push (VAPID)

### 연동 목적
- 브라우저 Web Push API를 통한 알림 발송

### 플로우
```
1. 프론트: navigator.serviceWorker + PushManager.subscribe()
   └─ VAPID 공개키로 PushSubscription 생성 (endpoint + p256dh + auth)
2. POST /notifications/subscribe
   └─ PushSubscription 엔티티 저장 (user_id 연결)
3. 서버 이벤트 발생 시:
   └─ VAPID 개인키로 서명 → endpoint에 HTTP POST
4. GNB NotificationDropdown에서 알림 목록 조회 (GET /notifications)
   └─ PATCH /notifications/{id}/read 로 읽음 처리
```

### 환경변수

| 변수명 | 설명 |
|--------|------|
| `VAPID_PUBLIC_KEY` | VAPID 공개키 (프론트 pushManager.subscribe에 전달) |
| `VAPID_PRIVATE_KEY` | VAPID 개인키 (서버에서 Push 서명용) |

---

## Google OAuth2 (소셜 로그인)

### 플로우
```
1. 프론트: window.location = /oauth2/authorization/google
2. Google 로그인 완료 → /login/oauth2/code/google 콜백
3. OAuth2SuccessHandler: 1회용 code 발급 → 프론트 /oauth/callback?code= 리다이렉트
4. OAuthCallbackPage: POST /auth/oauth2/exchange {code} → accessToken 저장
```

### 환경변수

| 변수명 | 설명 |
|--------|------|
| `GOOGLE_CLIENT_ID` | Google Cloud OAuth2 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google Cloud OAuth2 클라이언트 시크릿 |
| `GOOGLE_REDIRECT_URI` | Google 콘솔 등록 리다이렉트 URI |
| `OAUTH2_REDIRECT_URI` | 프론트 `/oauth/callback` 전체 URL |
