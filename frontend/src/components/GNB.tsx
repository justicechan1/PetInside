import { useNavigate } from 'react-router-dom';

export default function GNB() {
    const navigate = useNavigate();

    return (
        <header style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '20px 40px', borderBottom: '1px solid var(--border)',
            background: 'var(--white)'
        }}>
            <div onClick={() => navigate('/')}
                 style={{ fontSize: 24, fontWeight: 800, color: 'var(--primary)', cursor: 'pointer' }}>
                Pet Inside
            </div>
            <nav style={{ display: 'flex', gap: 30 }}>
                <span onClick={() => navigate('/qna')} style={{ cursor: 'pointer', fontWeight: 600 }}>Q&A</span>
                <span onClick={() => navigate('/boast')} style={{ cursor: 'pointer', fontWeight: 600 }}>자랑하기</span>
            </nav>
            <div style={{ display: 'flex', gap: 15 }}>
                <button onClick={() => navigate('/mypage')}
                        style={{ padding: '8px 16px', border: '1px solid var(--border)', borderRadius: 8, background: 'var(--white)', cursor: 'pointer' }}>
                    마이페이지
                </button>
                <button
                    style={{ padding: '8px 16px', border: 'none', borderRadius: 8, background: 'var(--primary)', color: 'white', fontWeight: 'bold', cursor: 'pointer' }}>
                    글쓰기
                </button>
            </div>
        </header>
    );
}