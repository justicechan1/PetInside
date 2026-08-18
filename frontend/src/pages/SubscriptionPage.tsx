import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as PortOne from '@portone/browser-sdk/v2';
import GNB from '../components/GNB';
import { prepareSubscription, completeSubscription } from '../api/subscriptionApi';
import type { SubscriptionCompleteResult } from '../api/subscriptionApi';
import { isAuthenticated } from '../utils/auth';

export default function SubscriptionPage() {
    const navigate = useNavigate();

    useEffect(() => {
        if (!isAuthenticated()) navigate('/login');
    }, [navigate]);

    const [fullName, setFullName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState<SubscriptionCompleteResult | null>(null);

    const handleSubscribe = async () => {
        setError('');
        if (!fullName.trim() || !phoneNumber.trim() || !email.trim()) {
            setError('이름, 연락처, 이메일을 모두 입력해주세요.');
            return;
        }

        setLoading(true);
        try {
            const prepared = await prepareSubscription();

            const issued = await PortOne.requestIssueBillingKey({
                storeId: prepared.storeId,
                channelKey: prepared.channelKey,
                billingKeyMethod: 'CARD',
                issueId: `issue-${prepared.paymentId}`,
                issueName: 'PetInside 구독 카드 등록',
                customer: { fullName, phoneNumber, email },
            });

            if (!issued || issued.code != null) {
                setError(issued?.message ?? '카드 등록에 실패했습니다.');
                return;
            }

            const completed = await completeSubscription(prepared.paymentId, issued.billingKey);
            setResult(completed);
        } catch (e: any) {
            setError(e.response?.data?.message ?? '구독 처리 중 오류가 발생했습니다.');
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

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 16px' }}>
                <h2 style={{ marginBottom: 20 }}>PetInside 구독</h2>

                {result ? (
                    <div style={cardStyle}>
                        <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 8 }}>구독이 시작되었습니다 🎉</p>
                        <p style={{ color: '#666', fontSize: 14 }}>구독 상태: {result.status}</p>
                        <p style={{ color: '#666', fontSize: 14 }}>
                            다음 결제일: {new Date(result.nextBillingAt).toLocaleString()}
                        </p>
                    </div>
                ) : (
                    <div style={cardStyle}>
                        <p style={{ color: '#666', fontSize: 14, marginBottom: 20 }}>
                            월 9,900원 정기결제로 카드를 등록합니다. 카드 등록 창이 뜨면 안내에 따라 진행해주세요.
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
                        <button onClick={handleSubscribe} disabled={loading} style={{
                            width: '100%', padding: '13px', border: 'none', borderRadius: 8,
                            background: 'var(--primary, #FF8C00)', color: '#fff', fontWeight: 700, fontSize: 15,
                            cursor: loading ? 'default' : 'pointer', opacity: loading ? 0.6 : 1,
                        }}>
                            {loading ? '처리 중...' : '구독 시작하기'}
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
