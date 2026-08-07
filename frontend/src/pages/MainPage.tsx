import GNB from '../components/GNB';

export default function MainPage() {
    return (
        <div>
            <GNB />
            <div style={{ padding: '40px' }}>
                <div style={{
                    background: '#FFF3E0', padding: '60px 40px',
                    borderRadius: 12, textAlign: 'center', marginBottom: 40
                }}>
                    <h1 style={{ color: 'var(--primary)', margin: '0 0 10px 0' }}>
                        우리 아이를 위한 모든 지식
                    </h1>
                    <p>집사들의 생생한 꿀팁과 귀여운 일상을 공유하세요!</p>
                </div>
            </div>
        </div>
    );
}