import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as PortOne from '@portone/browser-sdk/v2';
import GNB from '../components/GNB';
import {
    prepareBillingKey, createBillingKey, createSubscription,
    prepareOneTimePurchase, completeOneTimePurchase,
    getMySubscription, getPaymentHistory,
} from '../api/subscriptionApi';
import type { SubscriptionResult, SubscriptionMeResult, PaymentHistoryItem } from '../api/subscriptionApi';
import { isAuthenticated } from '../utils/auth';

type Mode = 'RECURRING' | 'ONE_TIME';

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
    const [history, setHistory] = useState<PaymentHistoryItem[]>([]);
    const [statusLoading, setStatusLoading] = useState(true);

    const loadStatus = async () => {
        setStatusLoading(true);
        try {
            const [subscription, payments] = await Promise.all([getMySubscription(), getPaymentHistory()]);
            setMySubscription(subscription);
            setHistory(payments);
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
                customer: { fullName, phoneNumber, email },
                // 모바일(REDIRECTION 전용 PG)에서 결제창이 이 URL로 복귀 - issueId는 우리가 붙인 쿼리라
                // PortOne이 결과 파라미터를 이어붙여도 그대로 남아있음
                redirectUrl: `${window.location.origin}/subscription/redirect?mode=billing-key&issueId=${encodeURIComponent(prepared.issueId)}`,
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
            loadStatus();
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
            });

            if (!paid || paid.code != null) {
                setError(paid?.message ?? '결제에 실패했습니다.');
                return;
            }

            const subscription = await completeOneTimePurchase(prepared.paymentId);
            setResult(subscription);
            loadStatus();
        } catch (e: any) {
            setError(e.response?.data?.message ?? '결제 처리 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
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
    const paymentStatusLabel: Record<PaymentHistoryItem['status'], string> = {
        READY: '준비중', PAID: '결제완료', FAILED: '실패',
    };

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 16px' }}>
                <h2 style={{ marginBottom: 20 }}>PetInside 구독</h2>

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
                        {mySubscription?.startAt && (
                            <p style={{ color: '#666', fontSize: 14 }}>
                                시작일: {new Date(mySubscription.startAt).toLocaleString()}
                            </p>
                        )}
                        <p style={{ color: '#666', fontSize: 14 }}>
                            {mySubscription?.type === 'RECURRING' ? '다음 결제일' : '이용 만료일'}:{' '}
                            {new Date(result?.nextBillingAt ?? mySubscription?.nextBillingAt ?? '').toLocaleString()}
                        </p>
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
                                ? '1,000원 결제로 1개월간 이용할 수 있어요. 자동으로 다시 결제되지 않고, 한 달 뒤 자동 만료돼요.'
                                : '월 1,000원 정기결제로 카드를 등록합니다. 카드 등록 창이 뜨면 안내에 따라 진행해주세요.'}
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
                    </div>
                )}

                {history.length > 0 && (
                    <div style={{ ...cardStyle, marginTop: 16 }}>
                        <p style={{ fontWeight: 700, fontSize: 15, marginBottom: 12 }}>결제 내역</p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                            {history.map(item => (
                                <div key={item.paymentId} style={{
                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                    padding: '10px 0', borderTop: '1px solid #f0f0f0',
                                }}>
                                    <div>
                                        <p style={{ fontSize: 14, fontWeight: 600 }}>
                                            {item.amount.toLocaleString()}{item.currency === 'KRW' ? '원' : ` ${item.currency}`}
                                        </p>
                                        <p style={{ fontSize: 12, color: '#999' }}>
                                            {new Date(item.paidAt ?? item.createdAt).toLocaleString()}
                                        </p>
                                    </div>
                                    <span style={{
                                        fontSize: 12, fontWeight: 700, padding: '4px 10px', borderRadius: 12,
                                        color: item.status === 'PAID' ? '#2e7d32' : item.status === 'FAILED' ? '#c62828' : '#666',
                                        background: item.status === 'PAID' ? '#e8f5e9' : item.status === 'FAILED' ? '#ffebee' : '#f0f0f0',
                                    }}>
                                        {paymentStatusLabel[item.status]}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
