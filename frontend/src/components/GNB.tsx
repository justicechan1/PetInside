import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function GNB() {
    const navigate = useNavigate();
    const isLoggedIn = !!localStorage.getItem('accessToken');
    const [dropdownOpen, setDropdownOpen] = useState(false);

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        setDropdownOpen(false);
        navigate('/');
        window.location.reload();
    };

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

            <div style={{ display: 'flex', gap: 15, alignItems: 'center' }}>
                {isLoggedIn ? (
                    <>
                        <button onClick={() => navigate('/posts/new')}
                                style={{ padding: '8px 16px', border: 'none', borderRadius: 8, background: 'var(--primary)', color: 'white', fontWeight: 'bold', cursor: 'pointer' }}>
                            글쓰기
                        </button>

                        {/* 프로필 동그라미 */}
                        <div style={{ position: 'relative' }}>
                            <div onClick={() => setDropdownOpen(!dropdownOpen)}
                                 style={{
                                     width: 40, height: 40, borderRadius: '50%',
                                     background: 'var(--primary)', color: 'white',
                                     display: 'flex', alignItems: 'center', justifyContent: 'center',
                                     cursor: 'pointer', fontWeight: 'bold', fontSize: 16
                                 }}>
                                👤
                            </div>

                            {/* 드롭다운 */}
                            {dropdownOpen && (
                                <div style={{
                                    position: 'absolute', right: 0, top: 48,
                                    background: 'var(--white)', border: '1px solid var(--border)',
                                    borderRadius: 8, boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                    minWidth: 140, zIndex: 100
                                }}>
                                    <div onClick={() => { navigate('/mypage'); setDropdownOpen(false); }}
                                         style={{ padding: '12px 16px', cursor: 'pointer', borderBottom: '1px solid var(--border)' }}>
                                        마이페이지
                                    </div>
                                    <div onClick={handleLogout}
                                         style={{ padding: '12px 16px', cursor: 'pointer', color: '#E03131' }}>
                                        로그아웃
                                    </div>
                                </div>
                            )}
                        </div>
                    </>
                ) : (
                    <button onClick={() => navigate('/login')}
                            style={{ padding: '8px 16px', border: 'none', borderRadius: 8, background: 'var(--primary)', color: 'white', fontWeight: 'bold', cursor: 'pointer' }}>
                        로그인
                    </button>
                )}
            </div>
        </header>
    );
}