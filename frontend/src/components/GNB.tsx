import { useEffect, useState } from 'react';
import axiosInstance from '../api/axiosInstance';
import { useNavigate } from 'react-router-dom';
import { getRoleFromToken } from '../utils/auth';

export default function GNB() {
    const navigate = useNavigate();
    const isLoggedIn = !!localStorage.getItem('accessToken');
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const [role, setRole] = useState<string | null>(null);
    const [nickname, setNickname] = useState<string | null>(() => localStorage.getItem('nickname'));

    useEffect(() => {
        setRole(getRoleFromToken());
    }, []);

    useEffect(() => {
        const onStorage = () => setNickname(localStorage.getItem('nickname'));
        window.addEventListener('storage', onStorage);
        return () => window.removeEventListener('storage', onStorage);
    }, []);


    const handleLogout = async () => {
        try {
            await axiosInstance.post('/api/v1/auth/logout');
        } catch {
            // accessToken이 이미 만료됐어도 클라이언트 쪽 로그아웃은 계속 진행
        }
        localStorage.removeItem('accessToken');
        localStorage.removeItem('nickname');
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
                <span onClick={() => navigate('/posts?category=QNA')} style={{ cursor: 'pointer', fontWeight: 600 }}>Q&A</span>
                <span onClick={() => navigate('/posts?category=BOAST')} style={{ cursor: 'pointer', fontWeight: 600 }}>자랑하기</span>
            </nav>

            <div style={{ display: 'flex', gap: 15, alignItems: 'center' }}>
                {isLoggedIn ? (
                    <>
                        <button onClick={() => navigate('/posts/new')}
                                style={{ padding: '8px 16px', border: 'none', borderRadius: 8, background: 'var(--primary)', color: 'white', fontWeight: 'bold', cursor: 'pointer' }}>
                            글쓰기
                        </button>

                        {role === 'ADMIN' && (
                            <button onClick={() => navigate('/admin')}
                                    style={{
                                        padding: '8px 16px',
                                        border: '1px solid var(--primary)',
                                        borderRadius: 8,
                                        background: 'white',
                                        color: 'var(--primary)',
                                        fontWeight: 'bold',
                                        cursor: 'pointer'
                                    }}>
                                관리자 페이지
                            </button>
                        )}

                        {/* 프로필 동그라미 */}
                        <div style={{ position: 'relative' }}>
                            <div onClick={() => setDropdownOpen(!dropdownOpen)}
                                 style={{
                                     width: 40, height: 40, borderRadius: '50%',
                                     background: 'var(--primary)', color: 'white',
                                     display: 'flex', alignItems: 'center', justifyContent: 'center',
                                     cursor: 'pointer', fontWeight: 'bold', fontSize: 16
                                 }}>
                                {nickname ? nickname[0].toUpperCase() : '👤'}
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