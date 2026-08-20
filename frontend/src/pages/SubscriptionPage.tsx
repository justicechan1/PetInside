import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as PortOne from '@portone/browser-sdk/v2';
import GNB from '../components/GNB';
import { baseURL } from '../api/axiosInstance';
import {
    prepareBillingKey, createBillingKey, createSubscription,
    prepareOneTimePurchase, completeOneTimePurchase,
    getMySubscription,
    cancelSubscription, resumeSubscription,
} from '../api/subscriptionApi';
import type { SubscriptionResult, SubscriptionMeResult } from '../api/subscriptionApi';
import { isAuthenticated, getUserIdFromToken } from '../utils/auth';

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
                // 우리 userId를 그대로 PortOne customerId로 넘김.
                customerId: getUserIdFromToken() ?? undefined,
                customer: { fullName, phoneNumber, email },
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

    const inputStyle: React.CSSProperties = {
        padding: '11px 14px', border: '1px solid #e0e0e0', borderRadius: 8,
        fontSize: 14, width: '100%', boxSizing: 'border-box', background: '#fff',
    };

    const cardStyle: React.CSSProperties = {
        background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)', padding: '24px 28px',
    };

    const tabBtnStyle = (active: boolean): React.CSSProperties => ({
        flex: 1, padding: '10px 0', border: 'none', borderRadius: 20, cursor: 'pointer', fontWeight: 600,
        background: active ? 'var(--primary, #FF8C00)' : '#f0f0f0',
        color: active ? '#fff' : '#333',
    });

    const isActiveSubscriber = mySubscription?.hasSubscription && mySubscription.status === 'ACTIVE';

    const benefitBoxStyle: React.CSSProperties = {
        marginTop: 16, padding: '14px 16px', borderRadius: 10,
        background: '#fff8ec', border: '1px solid #ffe1b3', fontSize: 13, color: '#9a6a1f',
    };

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                    <h2 style={{ margin: 0 }}>PetInside 구독</h2>
                    <button onClick={() => navigate('/subscription/history')} style={{
                        border: 'none', background: 'none', color: '#666', fontSize: 13,
                        fontWeight: 600, cursor: 'pointer', textDecoration: 'underline',
                    }}>
                        결제 내역
                    </button>
                </div>

                {statusLoading ? (
                    <div style={cardStyle}>
                        <p style={{ color: '#666', fontSize: 14 }}>불러오는 중...</p>
                    </div>
                ) : result || isActiveSubscriber ? (
                    <div style={cardStyle}>
                        <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 8 }}>
                            {mySubscription?.type === 'RECURRING' ? '정기결제 구독 중 🎉' : '이용권 이용 중 🎉'}
                        </p>
                        <p style={{ color: '#666', fontSize: 14 }}>상태: {result?.status ?? mySubscription?.status}</p>
                        {mySubscription?.startAt && mySubscription?.nextBillingAt && (
                            <>
                                <p style={{ color: '#666', fontSize: 14 }}>
                                    결제일: {formatKoreanDate(mySubscription.startAt)}
                                </p>
                                <p style={{ color: '#666', fontSize: 14 }}>
                                    구독 기간: {formatISODate(mySubscription.startAt)} ~ {formatISODate(mySubscription.nextBillingAt)}
                                </p>
                                {mySubscription.type === 'RECURRING' && (
                                    <p style={{ color: '#666', fontSize: 14 }}>
                                        다음 결제일: {formatISODate(addDays(mySubscription.nextBillingAt, 1))}
                                    </p>
                                )}
                            </>
                        )}

                        <div style={benefitBoxStyle}>
                            구독 혜택은 현재 준비 중이에요. 확정되는 대로 이 화면에서 안내해드릴게요.
                        </div>

                        {mySubscription?.type === 'RECURRING' && (
                            <div style={{ marginTop: 16, paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
                                {mySubscription.canceledAt ? (
                                    <>
                                        <p style={{ fontSize: 13, color: '#f44336', marginBottom: 10 }}>
                                            해지 예약됨 — 다음 결제일 이후 자동 만료됩니다.
                                        </p>
                                        <button onClick={handleResume} disabled={cancelLoading} style={{
                                            width: '100%', padding: '11px', border: '1px solid var(--primary, #FF8C00)',
                                            borderRadius: 8, background: '#fff', color: 'var(--primary, #FF8C00)',
                                            fontWeight: 700, fontSize: 14, cursor: cancelLoading ? 'default' : 'pointer',
                                        }}>
                                            {cancelLoading ? '처리 중...' : '구독 재개하기'}
                                        </button>
                                    </>
                                ) : (
                                    <button onClick={handleCancel} disabled={cancelLoading} style={{
                                        width: '100%', padding: '11px', border: '1px solid #e0e0e0',
                                        borderRadius: 8, background: '#fff', color: '#666',
                                        fontWeight: 700, fontSize: 14, cursor: cancelLoading ? 'default' : 'pointer',
                                    }}>
                                        {cancelLoading ? '처리 중...' : '구독 해지하기'}
                                    </button>
                                )}
                                {error && (
                                    <p style={{ margin: '10px 0 0', fontSize: 13, color: '#f44336' }}>{error}</p>
                                )}
                            </div>
                        )}
                    </div>
                ) : (
                    <div style={cardStyle}>
                        <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                            <button style={tabBtnStyle(mode === 'ONE_TIME')} onClick={() => { setMode('ONE_TIME'); setError(''); }}>
                                1개월 이용권 (단건)
                            </button>
                            <button style={tabBtnStyle(mode === 'RECURRING')} onClick={() => { setMode('RECURRING'); setError(''); }}>
                                정기결제 (자동갱신)
                            </button>
                        </div>

                        <p style={{ color: '#666', fontSize: 14, marginBottom: 20 }}>
                            {mode === 'ONE_TIME'
                                ? '1,900원 결제로 1개월간 이용할 수 있어요. 자동으로 다시 결제되지 않고, 한 달 뒤 자동 만료돼요.'
                                : '월 1,900원 정기결제로 카드를 등록합니다. 카드 등록 창이 뜨면 안내에 따라 진행해주세요.'}
                        </p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 16 }}>
                            <input value={fullName} onChange={e => setFullName(e.target.value)}
                                placeholder="이름" style={inputStyle} />
                            <input value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)}
                                placeholder="연락처 (예: 010-1234-5678)" style={inputStyle} />
                            <input value={email} onChange={e => setEmail(e.target.value)}
                                placeholder="이메일" style={inputStyle} />
                        </div>
                        {error && (
                            <p style={{ margin: '0 0 12px', fontSize: 13, color: '#f44336' }}>{error}</p>
                        )}
                        <button
                            onClick={mode === 'ONE_TIME' ? handleOneTimePurchase : handleSubscribe}
                            disabled={loading}
                            style={{
                                width: '100%', padding: '13px', border: 'none', borderRadius: 8,
                                background: 'var(--primary, #FF8C00)', color: '#fff', fontWeight: 700, fontSize: 15,
                                cursor: loading ? 'default' : 'pointer', opacity: loading ? 0.6 : 1,
                            }}>
                            {loading ? '처리 중...' : mode === 'ONE_TIME' ? '1개월 이용권 구매하기' : '구독 시작하기'}
                        </button>

                        <div style={benefitBoxStyle}>
                            구독하면 어떤 혜택이 있는지는 현재 준비 중이에요. 확정되는 대로 안내해드릴게요.
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
