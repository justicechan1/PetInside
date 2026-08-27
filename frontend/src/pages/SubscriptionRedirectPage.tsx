import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { createBillingKey, createSubscription, completeOneTimePurchase } from '../api/subscriptionApi';

// 모바일 브라우저에서는 PortOne 결제창이 팝업이 아니라 페이지 이동(REDIRECTION) 방식으로 동작해서,
// 결제/카드등록이 끝나면 여기로 돌아온다. SubscriptionPage의 Promise 분기와 같은 완료 처리를 이어서 한다.
export default function SubscriptionRedirectPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState<'processing' | 'success' | 'error'>('processing');
    const [message, setMessage] = useState('결제 결과를 확인하는 중...');
    // useSearchParams()는 렌더마다 새 객체를 반환해서 effect의 searchParams 의존성이 매 렌더 "변경"으로
    // 잡힌다. run() 안에서 setStatus/setMessage로 리렌더가 일어나면 effect가 재실행돼 complete API가
    // 중복 호출(-> 이미 처리된 결제입니다 409)되므로, 최초 1회만 실행되도록 가드.
    const hasRun = useRef(false);

    useEffect(() => {
        if (hasRun.current) return;
        hasRun.current = true;

        const run = async () => {
            const mode = searchParams.get('mode');
            const code = searchParams.get('code');
            const failureMessage = searchParams.get('message');

            if (code != null) {
                setStatus('error');
                setMessage(failureMessage ?? '결제에 실패했습니다.');
                return;
            }

            try {
                if (mode === 'one-time') {
                    const paymentId = searchParams.get('paymentId');
                    if (!paymentId) throw new Error('paymentId 없음');
                    await completeOneTimePurchase(paymentId);
                } else if (mode === 'billing-key') {
                    const issueId = searchParams.get('issueId');
                    const billingKey = searchParams.get('billingKey');
                    if (!issueId || !billingKey) throw new Error('issueId/billingKey 없음');
                    const created = await createBillingKey(issueId, billingKey);
                    await createSubscription(created.billingKeyId);
                } else {
                    throw new Error('알 수 없는 모드');
                }
                setStatus('success');
                setMessage('처리가 완료되었습니다.');
            } catch (e: any) {
                setStatus('error');
                setMessage(e.response?.data?.message ?? '처리 중 오류가 발생했습니다.');
            }
        };

        run();
    }, [searchParams]);

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 16px', textAlign: 'center' }}>
                <div style={{
                    background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.06)', padding: '32px 24px',
                }}>
                    <p style={{ fontSize: 15, color: status === 'error' ? '#f44336' : '#333', marginBottom: 20 }}>
                        {message}
                    </p>
                    {status !== 'processing' && (
                        <button onClick={() => navigate('/subscription')} style={{
                            padding: '11px 24px', border: 'none', borderRadius: 8,
                            background: 'var(--primary, #FF8C00)', color: '#fff', fontWeight: 700,
                            cursor: 'pointer',
                        }}>
                            구독 페이지로 이동
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
