import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as PortOne from '@portone/browser-sdk/v2';
import GNB from '../components/GNB';
import { baseURL } from '../api/axiosInstance';
import {
    prepareBillingKey, createBillingKey, createSubscription,
    prepareOneTimePurchase, completeOneTimePurchase,
    getMySubscription,
    cancelSubscription, resumeSubscription, retrySubscriptionPayment,
} from '../api/subscriptionApi';
import type { SubscriptionResult, SubscriptionMeResult } from '../api/subscriptionApi';
import { isAuthenticated, getUserIdFromToken } from '../utils/auth';
import petProfileRegisterImg from '../assets/benefit-pet-profile-register.png';
import petProfileFeedImg from '../assets/benefit-pet-profile-feed.png';

type Mode = 'RECURRING' | 'ONE_TIME';

const formatKoreanDate = (iso: string) => {
    const d = new Date(iso);
    return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
};

const formatISODate = (iso: string) => {
    const d = new Date(iso);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

const addDays = (iso: string, days: number) => {
    const d = new Date(iso);
    d.setDate(d.getDate() + days);
    return d.toISOString();
};

export default function SubscriptionPage() {
    const navigate = useNavigate();

    useEffect(() => {
        if (!isAuthenticated()) navigate('/login');
    }, [navigate]);

    const [mode, setMode] = useState<Mode>('ONE_TIME');
    const [fullName, setFullName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState<SubscriptionResult | null>(null);

    const [mySubscription, setMySubscription] = useState<SubscriptionMeResult | null>(null);
    const [statusLoading, setStatusLoading] = useState(true);
    const [cancelLoading, setCancelLoading] = useState(false);
    const [retryLoading, setRetryLoading] = useState(false);
    const [lightboxSrc, setLightboxSrc] = useState<string | null>(null);

    const loadStatus = async () => {
        setStatusLoading(true);
        try {
            setMySubscription(await getMySubscription());
        } catch {
            // 조회 실패는 조용히 무시하고 구매 폼을 그대로 보여준다
        } finally {
            setStatusLoading(false);
        }
    };

    useEffect(() => {
        if (isAuthenticated()) loadStatus();
    }, []);

    const handleSubscribe = async () => {
        setError('');
        if (!fullName.trim() || !phoneNumber.trim() || !email.trim()) {
            setError('이름, 연락처, 이메일을 모두 입력해주세요.');
            return;
        }

        setLoading(true);
        try {
            // 1. 카드 등록(빌링키 발급) 시도 사실을 먼저 서버에 남김 - 이 SDK 호출 도중 이탈해도
            //    BillingKey.Issued 웹훅 + issueId로 서버가 복구할 수 있도록 하기 위함
            const prepared = await prepareBillingKey();

            const issued = await PortOne.requestIssueBillingKey({
                storeId: prepared.storeId,
                channelKey: prepared.channelKey,
                billingKeyMethod: 'CARD',
                issueId: prepared.issueId,
                issueName: 'PetInside 구독 카드 등록',
                // 백엔드가 발급 의도(intent)를 남긴 사용자와 실제 발급받은 사용자가 같은지 대조할 수 있도록,
                // 우리 userId를 그대로 PortOne customer.customerId로 넘김.
                customer: { customerId: getUserIdFromToken() ?? undefined, fullName, phoneNumber, email },
                // 모바일(REDIRECTION 전용 PG)에서 결제창이 이 URL로 복귀 - issueId는 우리가 붙인 쿼리라
                // PortOne이 결과 파라미터를 이어붙여도 그대로 남아있음
                redirectUrl: `${window.location.origin}/subscription/redirect?mode=billing-key&issueId=${encodeURIComponent(prepared.issueId)}`,
                // 콘솔에 웹훅 URL을 등록하지 않아도, 이 결제 건에 한해 이 주소로 웹훅을 보내달라고 개별 지정
                noticeUrls: [`${baseURL}/api/v1/payments/webhook`],
            });

            if (!issued || issued.code != null) {
                setError(issued?.message ?? '카드 등록에 실패했습니다.');
                return;
            }

            // 2. 발급된 빌링키를 서버가 재조회로 검증하고 암호화 저장
            const billingKey = await createBillingKey(prepared.issueId, issued.billingKey);

            // 3. 검증된 빌링키로 1회차 결제 실행 + 구독 시작
            const subscription = await createSubscription(billingKey.billingKeyId);
            setResult(subscription);
            await loadStatus();
        } catch (e: any) {
            setError(e.response?.data?.message ?? '구독 처리 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleOneTimePurchase = async () => {
        setError('');
        if (!fullName.trim() || !phoneNumber.trim() || !email.trim()) {
            setError('이름, 연락처, 이메일을 모두 입력해주세요.');
            return;
        }

        setLoading(true);
        try {
            const prepared = await prepareOneTimePurchase();

            const paid = await PortOne.requestPayment({
                storeId: prepared.storeId,
                channelKey: prepared.channelKey,
                paymentId: prepared.paymentId,
                orderName: 'PetInside 1개월 이용권',
                totalAmount: prepared.amount,
                currency: 'CURRENCY_KRW',
                payMethod: 'CARD',
                customer: { fullName, phoneNumber, email },
                redirectUrl: `${window.location.origin}/subscription/redirect?mode=one-time`,
                noticeUrls: [`${baseURL}/api/v1/payments/webhook`],
            });

            if (!paid || paid.code != null) {
                setError(paid?.message ?? '결제에 실패했습니다.');
                return;
            }

            const subscription = await completeOneTimePurchase(prepared.paymentId);
            setResult(subscription);
            await loadStatus();
        } catch (e: any) {
            setError(e.response?.data?.message ?? '결제 처리 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async () => {
        if (!mySubscription?.subscriptionId) return;
        setCancelLoading(true);
        setError('');
        try {
            await cancelSubscription(mySubscription.subscriptionId);
            await loadStatus();
        } catch (e: any) {
            setError(e.response?.data?.message ?? '구독 해지 처리 중 오류가 발생했습니다.');
        } finally {
            setCancelLoading(false);
        }
    };

    const handleResume = async () => {
        if (!mySubscription?.subscriptionId) return;
        setCancelLoading(true);
        setError('');
        try {
            await resumeSubscription(mySubscription.subscriptionId);
            await loadStatus();
        } catch (e: any) {
            setError(e.response?.data?.message ?? '구독 재개 처리 중 오류가 발생했습니다.');
        } finally {
            setCancelLoading(false);
        }
    };

    const handleRetryPayment = async () => {
        if (!mySubscription?.subscriptionId) return;
        setRetryLoading(true);
        setError('');
        try {
            const updated = await retrySubscriptionPayment(mySubscription.subscriptionId);
            setMySubscription(updated);
            if (updated.status === 'PAST_DUE') {
                setError('결제가 아직 실패 상태예요. 카드 정보를 확인한 뒤 다시 시도해주세요.');
            }
        } catch (e: any) {
            setError(e.response?.data?.message ?? '결제 재시도 중 오류가 발생했습니다.');
        } finally {
            setRetryLoading(false);
        }
    };

    // ─── 디자인 토큰 ───────────────────────────────────────────────────
    const primary = 'var(--primary, #FF8C00)';
    const ink = '#22262B';
    const muted = '#767C86';
    const border = '#ECEAE6';
    const page = '#FAF9F7';

    const inputStyle: React.CSSProperties = {
        padding: '13px 15px', border: `1px solid ${border}`, borderRadius: 12,
        fontSize: 14, width: '100%', boxSizing: 'border-box', background: '#fff',
        outline: 'none',
    };

    const cardStyle: React.CSSProperties = {
        background: '#fff', borderRadius: 24, border: `1px solid ${border}`,
        boxShadow: '0 1px 2px rgba(20,16,8,0.03), 0 12px 32px -16px rgba(20,16,8,0.10)',
        padding: '30px 28px',
    };

    const tabBtnStyle = (active: boolean): React.CSSProperties => ({
        flex: 1, padding: '11px 0', border: 'none', borderRadius: 12, cursor: 'pointer',
        fontWeight: 700, fontSize: 14, transition: 'background .15s, color .15s',
        background: active ? primary : '#F2F1EE',
        color: active ? '#fff' : muted,
    });

    const primaryBtnStyle = (disabled: boolean): React.CSSProperties => ({
        border: 'none', borderRadius: 14, background: primary, color: '#fff',
        fontWeight: 700, fontSize: 15, cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.55 : 1, transition: 'opacity .15s',
    });

    const outlineBtnStyle = (disabled: boolean): React.CSSProperties => ({
        border: `1px solid ${border}`, borderRadius: 14, background: '#fff', color: ink,
        fontWeight: 700, fontSize: 15, cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.55 : 1, transition: 'opacity .15s, border-color .15s',
    });

    const isActiveSubscriber = mySubscription?.hasSubscription && mySubscription.status === 'ACTIVE';
    const isPastDue = mySubscription?.hasSubscription && mySubscription.status === 'PAST_DUE';

    // 혜택을 스크린샷과 함께 자세히 소개하는 영역. 구독 상태와 무관하게 항상 별도 카드로 보여줌.
    // screenshots가 있는 혜택만 이미지로 보여주고, 아직 없는 혜택은 아이콘+설명 줄로만 표시.
    // 스크린샷이 더 생기면 해당 혜택의 screenshots 배열만 채우면 됨.
    const BENEFIT_DETAILS: {
        icon: string; title: string; description: string;
        screenshots?: { src: string; caption: string }[];
    }[] = [
        {
            icon: '🐾', title: '반려동물 프로필',
            description: '내 반려동물의 프로필을 만들고, 사진을 인스타 피드처럼 모아서 자랑할 수 있어요.',
            screenshots: [
                { src: petProfileRegisterImg, caption: '반려동물 등록' },
                { src: petProfileFeedImg, caption: '프로필 & 사진 피드' },
            ],
        },
        { icon: '😺', title: '프리미엄 이모티콘', description: '채팅과 게시글에서 쓸 수 있는 PetInside 전용 이모티콘이 열려요.' },
        { icon: '✅', title: '인증 뱃지', description: '닉네임 옆에 프리미엄 멤버십 인증 뱃지가 붙어요.' },
        { icon: '🎨', title: '프로필 커스터마이징', description: '프로필 테마와 배경을 취향대로 꾸밀 수 있어요.' },
    ];

    const renderBenefitShowcase = () => (
        <div>
            <p style={{ margin: '0 0 14px', fontSize: 12, fontWeight: 800, letterSpacing: '0.04em', color: muted, textTransform: 'uppercase' }}>
                이런 혜택이 있어요
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 14 }}>
                {BENEFIT_DETAILS.map(b => (
                    <div key={b.title} style={{ padding: '16px', borderRadius: 16, background: '#FFF8EC' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                            <span style={{
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                width: 30, height: 30, borderRadius: '50%', background: '#fff',
                                fontSize: 15, flexShrink: 0,
                            }}>{b.icon}</span>
                            <span style={{ fontSize: 14, fontWeight: 800, color: '#8A5A16' }}>{b.title}</span>
                        </div>
                        <p style={{ margin: '0 0 10px', fontSize: 12.5, color: '#9A6A1F', lineHeight: 1.5 }}>
                            {b.description}
                        </p>
                        {b.screenshots && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                                {b.screenshots.map(s => (
                                    <div key={s.caption}>
                                        <img src={s.src} alt={s.caption} onClick={() => setLightboxSrc(s.src)} style={{
                                            width: '100%', display: 'block', borderRadius: 14,
                                            border: '1px solid #FFE1B3', background: '#fff', cursor: 'zoom-in',
                                        }} />
                                        <p style={{ margin: '6px 0 0', fontSize: 11, color: '#9A6A1F', textAlign: 'center', fontWeight: 600 }}>
                                            {s.caption}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );

    const InfoRow = ({ icon, label, value }: { icon: string; label: string; value: string }) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0' }}>
            <span style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                width: 34, height: 34, borderRadius: 10, background: page, fontSize: 15, flexShrink: 0,
            }}>{icon}</span>
            <span style={{ fontSize: 13, color: muted, flex: 1 }}>{label}</span>
            <span style={{ fontSize: 13.5, color: ink, fontWeight: 700 }}>{value}</span>
        </div>
    );

    return (
        <div style={{ background: page, minHeight: '100vh' }}>
            <GNB />
            <div style={{ padding: '40px 16px 64px' }}>
            <div style={{ maxWidth: 460, margin: '0 auto' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 22 }}>
                    <div>
                        <p style={{ margin: '0 0 4px', fontSize: 12, fontWeight: 800, letterSpacing: '0.08em', color: primary, textTransform: 'uppercase' }}>
                            PetInside
                        </p>
                        <h1 style={{ margin: 0, fontSize: 24, fontWeight: 800, color: ink, letterSpacing: '-0.01em' }}>
                            프리미엄 멤버십
                        </h1>
                    </div>
                    <button onClick={() => navigate('/subscription/history')} style={{
                        border: 'none', background: 'none', color: muted, fontSize: 13,
                        fontWeight: 700, cursor: 'pointer', padding: 0,
                    }}>
                        결제 내역 →
                    </button>
                </div>

                {statusLoading ? (
                    <div style={cardStyle}>
                        <p style={{ color: muted, fontSize: 14, margin: 0 }}>불러오는 중...</p>
                    </div>
                ) : result || isActiveSubscriber ? (
                    <div style={cardStyle}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 22 }}>
                            <div style={{
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                width: 52, height: 52, borderRadius: 16, flexShrink: 0,
                                background: 'linear-gradient(135deg, #FFD9A0, #FF8C00)', fontSize: 24,
                            }}>🐾</div>
                            <div>
                                <p style={{ margin: 0, fontWeight: 800, fontSize: 17, color: ink }}>
                                    {mySubscription?.type === 'RECURRING' ? '정기결제 이용 중' : '1개월 이용권 이용 중'}
                                </p>
                                <p style={{ margin: '2px 0 0', fontSize: 13, color: muted }}>
                                    PetInside 프리미엄 멤버십
                                </p>
                            </div>
                        </div>

                        {mySubscription?.startAt && mySubscription?.nextBillingAt && (
                            <div style={{ marginBottom: 22 }}>
                                <InfoRow icon="💳" label="결제일" value={formatKoreanDate(mySubscription.startAt)} />
                                <InfoRow icon="📅" label="이용 기간"
                                    value={`${formatISODate(mySubscription.startAt)} ~ ${formatISODate(mySubscription.nextBillingAt)}`} />
                                {mySubscription.type === 'RECURRING' && !mySubscription.canceledAt && (
                                    <InfoRow icon="🔄" label="다음 결제일"
                                        value={formatISODate(addDays(mySubscription.nextBillingAt, 1))} />
                                )}
                            </div>
                        )}

                        {mySubscription?.type === 'RECURRING' && (
                            <div style={{ marginTop: 24 }}>
                                {mySubscription.canceledAt ? (
                                    <>
                                        <div style={{
                                            padding: '12px 14px', borderRadius: 12, background: '#FFF3E0',
                                            fontSize: 13, color: '#B85C00', fontWeight: 600, marginBottom: 12,
                                        }}>
                                            해지 예약됨 — 다음 결제일 이후 자동 만료됩니다.
                                        </div>
                                        <button onClick={handleResume} disabled={cancelLoading}
                                            style={{ ...outlineBtnStyle(cancelLoading), width: '100%', padding: '13px', borderColor: primary, color: primary }}>
                                            {cancelLoading ? '처리 중...' : '구독 재개하기'}
                                        </button>
                                    </>
                                ) : (
                                    <button onClick={handleCancel} disabled={cancelLoading}
                                        style={{ ...outlineBtnStyle(cancelLoading), width: '100%', padding: '13px' }}>
                                        {cancelLoading ? '처리 중...' : '구독 해지하기'}
                                    </button>
                                )}
                                {error && (
                                    <p style={{ margin: '10px 0 0', fontSize: 13, color: '#e03131' }}>{error}</p>
                                )}
                            </div>
                        )}
                    </div>
                ) : isPastDue ? (
                    <div style={cardStyle}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 16 }}>
                            <div style={{
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                width: 52, height: 52, borderRadius: 16, flexShrink: 0,
                                background: '#FFF0F0', fontSize: 24,
                            }}>😥</div>
                            <div>
                                <p style={{ margin: 0, fontWeight: 800, fontSize: 17, color: '#C92A2A' }}>
                                    결제에 실패했어요
                                </p>
                                <p style={{ margin: '2px 0 0', fontSize: 13, color: muted }}>
                                    카드 상태를 확인하고 다시 결제해주세요
                                </p>
                            </div>
                        </div>
                        <p style={{ fontSize: 13.5, color: muted, lineHeight: 1.6, margin: '0 0 22px' }}>
                            계속 실패하면 구독이 자동으로 종료돼요. 등록된 카드의 한도·잔액을 확인한 뒤 [다시 결제]로 재시도해보세요.
                        </p>
                        {error && (
                            <p style={{ margin: '0 0 12px', fontSize: 13, color: '#e03131' }}>{error}</p>
                        )}
                        <div style={{ display: 'flex', gap: 10 }}>
                            <button onClick={handleCancel} disabled={cancelLoading}
                                style={{ ...outlineBtnStyle(cancelLoading), flex: 1, padding: '13px' }}>
                                {cancelLoading ? '처리 중...' : '구독 취소'}
                            </button>
                            <button onClick={handleRetryPayment} disabled={retryLoading}
                                style={{ ...primaryBtnStyle(retryLoading), flex: 1, padding: '13px' }}>
                                {retryLoading ? '재시도 중...' : '다시 결제'}
                            </button>
                        </div>
                    </div>
                ) : (
                    <div style={cardStyle}>
                        <div style={{ display: 'flex', gap: 8, marginBottom: 22 }}>
                            <button style={tabBtnStyle(mode === 'ONE_TIME')} onClick={() => { setMode('ONE_TIME'); setError(''); }}>
                                1개월 이용권
                            </button>
                            <button style={tabBtnStyle(mode === 'RECURRING')} onClick={() => { setMode('RECURRING'); setError(''); }}>
                                정기결제
                            </button>
                        </div>

                        <p style={{ color: muted, fontSize: 13.5, lineHeight: 1.6, margin: '0 0 22px' }}>
                            {mode === 'ONE_TIME'
                                ? '1,900원 결제로 1개월간 이용할 수 있어요. 자동으로 다시 결제되지 않고, 한 달 뒤 자동 만료돼요.'
                                : '월 1,900원 정기결제로 카드를 등록합니다. 카드 등록 창이 뜨면 안내에 따라 진행해주세요.'}
                        </p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 18 }}>
                            <input value={fullName} onChange={e => setFullName(e.target.value)}
                                placeholder="이름" style={inputStyle} />
                            <input value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)}
                                placeholder="연락처 (예: 010-1234-5678)" style={inputStyle} />
                            <input value={email} onChange={e => setEmail(e.target.value)}
                                placeholder="이메일" style={inputStyle} />
                        </div>
                        {error && (
                            <p style={{ margin: '0 0 12px', fontSize: 13, color: '#e03131' }}>{error}</p>
                        )}
                        <button
                            onClick={mode === 'ONE_TIME' ? handleOneTimePurchase : handleSubscribe}
                            disabled={loading}
                            style={{ ...primaryBtnStyle(loading), width: '100%', padding: '14px', marginBottom: 26 }}>
                            {loading ? '처리 중...' : mode === 'ONE_TIME' ? '1개월 이용권 구매하기' : '구독 시작하기'}
                        </button>
                    </div>
                )}

            </div>

            {!statusLoading && (
                <div style={{ maxWidth: 640, margin: '16px auto 0' }}>
                    <div style={cardStyle}>
                        {renderBenefitShowcase()}
                    </div>
                </div>
            )}
            </div>

            {lightboxSrc && (
                <div onClick={() => setLightboxSrc(null)} style={{
                    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.88)', zIndex: 1000,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16, cursor: 'zoom-out',
                }}>
                    <button onClick={() => setLightboxSrc(null)} style={{
                        position: 'absolute', top: 16, right: 16, background: 'none', border: 'none',
                        color: '#fff', fontSize: 28, cursor: 'pointer', lineHeight: 1,
                    }}>×</button>
                    <img src={lightboxSrc} alt="" onClick={e => e.stopPropagation()} style={{
                        maxWidth: '100%', maxHeight: '85vh', objectFit: 'contain', borderRadius: 12, display: 'block', cursor: 'default',
                    }} />
                </div>
            )}
        </div>
    );
}
