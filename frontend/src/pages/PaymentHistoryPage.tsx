import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GNB from '../components/GNB';
import { getPaymentHistory } from '../api/subscriptionApi';
import type { PaymentHistoryItem } from '../api/subscriptionApi';
import { isAuthenticated } from '../utils/auth';

const paymentStatusLabel: Record<PaymentHistoryItem['status'], string> = {
    READY: '준비중', PAID: '결제완료', FAILED: '실패',
};

export default function PaymentHistoryPage() {
    const navigate = useNavigate();

    useEffect(() => {
        if (!isAuthenticated()) navigate('/login');
    }, [navigate]);

    const [history, setHistory] = useState<PaymentHistoryItem[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!isAuthenticated()) return;
        (async () => {
            setLoading(true);
            try {
                setHistory(await getPaymentHistory());
            } catch {
                // 조회 실패는 조용히 무시하고 빈 목록을 보여준다
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const cardStyle: React.CSSProperties = {
        background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)', padding: '28px 32px',
    };

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 640, margin: '0 auto', padding: '32px 20px' }}>
                <h2 style={{ marginBottom: 24, fontSize: 24 }}>결제 내역</h2>

                <div style={cardStyle}>
                    {loading ? (
                        <p style={{ color: '#666', fontSize: 15 }}>불러오는 중...</p>
                    ) : history.length === 0 ? (
                        <p style={{ color: '#666', fontSize: 15 }}>결제 내역이 없습니다.</p>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                            {history.map((item, index) => (
                                <div key={item.paymentId} style={{
                                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                    padding: '18px 4px', borderTop: index === 0 ? 'none' : '1px solid #f0f0f0',
                                }}>
                                    <div>
                                        <p style={{ fontSize: 18, fontWeight: 700 }}>
                                            {item.amount.toLocaleString()}{item.currency === 'KRW' ? '원' : ` ${item.currency}`}
                                        </p>
                                        <p style={{ fontSize: 14, color: '#999', marginTop: 4 }}>
                                            {new Date(item.paidAt ?? item.createdAt).toLocaleString()}
                                        </p>
                                    </div>
                                    <span style={{
                                        fontSize: 14, fontWeight: 700, padding: '6px 14px', borderRadius: 14,
                                        color: item.status === 'PAID' ? '#2e7d32' : item.status === 'FAILED' ? '#c62828' : '#666',
                                        background: item.status === 'PAID' ? '#e8f5e9' : item.status === 'FAILED' ? '#ffebee' : '#f0f0f0',
                                    }}>
                                        {paymentStatusLabel[item.status]}
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <button onClick={() => navigate('/subscription')} style={{
                    width: '100%', marginTop: 16, padding: '14px', border: '1px solid #e0e0e0',
                    borderRadius: 8, background: '#fff', color: '#666', fontWeight: 700, fontSize: 15,
                    cursor: 'pointer',
                }}>
                    구독 화면으로 이동
                </button>
            </div>
        </div>
    );
}
