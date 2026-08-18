import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as PortOne from '@portone/browser-sdk/v2';
import GNB from '../components/GNB';
import {
    prepareBillingKey, createBillingKey, createSubscription,
    prepareOneTimePurchase, completeOneTimePurchase,
} from '../api/subscriptionApi';
import type { SubscriptionResult } from '../api/subscriptionApi';
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
                // 카드결제창에 간편결제(카카오페이 등)가 같이 노출되면 실수로 실제 결제가
                // 발생할 수 있어(테스트 채널에서도 간편결제는 실결제로 처리됨) 꺼둔다.
                bypass: { inicis_v2: { acceptmethod: ['noeasypay'] } },
            });

            if (!paid || paid.code != null) {
                setError(paid?.message ?? '결제에 실패했습니다.');
                return;
            }

            const subscription = await completeOneTimePurchase(prepared.paymentId);
            setResult(subscription);
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

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 16px' }}>
                <h2 style={{ marginBottom: 20 }}>PetInside 구독</h2>

                {result ? (
                    <div style={cardStyle}>
                        <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 8 }}>
                            {mode === 'ONE_TIME' ? '이용권이 시작되었습니다 🎉' : '구독이 시작되었습니다 🎉'}
                        </p>
                        <p style={{ color: '#666', fontSize: 14 }}>상태: {result.status}</p>
                        <p style={{ color: '#666', fontSize: 14 }}>
                            {mode === 'ONE_TIME' ? '이용 만료일' : '다음 결제일'}: {new Date(result.nextBillingAt).toLocaleString()}
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
            </div>
        </div>
    );
}
