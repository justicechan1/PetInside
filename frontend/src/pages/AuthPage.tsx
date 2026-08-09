import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance, { baseURL } from '../api/axiosInstance';

export default function AuthPage() {
    const navigate = useNavigate();
    const [tab, setTab] = useState<'login' | 'signup'>('login');
    const [loginForm, setLoginForm] = useState({ username: '', password: REDACTED });
    const [signupForm, setSignupForm] = useState({ username: '', nickname: '', password: REDACTED });

    const handleLogin = async () => {
        try {
            const res = await axiosInstance.post('/api/v1/auth/login', loginForm);
            localStorage.setItem('accessToken', res.data.data.accessToken);
            localStorage.setItem('refreshToken', res.data.data.refreshToken);
            const me = await axiosInstance.get('/api/v1/users/me');
            localStorage.setItem('nickname', me.data.data.nickname);
            navigate('/');
        } catch (e: any) {
            alert(e.response?.data?.message ?? '로그인에 실패했습니다.');
        }
    };

    const handleGoogleLogin = () => {
        window.location.href = `${baseURL}/oauth2/authorization/google`;
    };

    const handleSignup = async () => {
        try {
            await axiosInstance.post('/api/v1/auth/signup', signupForm);
            alert('회원가입 완료! 로그인해주세요.');
            setTab('login');
        } catch (e: any) {
            alert(e.response?.data?.message ?? '회원가입에 실패했습니다.');
        }
    };

    const inputStyle = {
        padding: 14, border: '1px solid var(--border)',
        borderRadius: 8, width: '100%', fontSize: 14
    };

    return (
        <div style={{ minHeight: '100vh', background: 'var(--bg-color)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ width: 400, background: 'var(--white)', borderRadius: 16, padding: 40, boxShadow: '0 10px 30px rgba(0,0,0,0.08)' }}>

                <div onClick={() => navigate('/')}
                     style={{ fontSize: 24, fontWeight: 800, color: 'var(--primary)', textAlign: 'center', marginBottom: 30, cursor: 'pointer' }}>
                    Pet Inside
                </div>

                {tab === 'login' ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                        <input placeholder="아이디" value={loginForm.username}
                               onChange={e => setLoginForm({ ...loginForm, username: e.target.value })}
                               style={inputStyle} />
                        <input type="password" placeholder="비밀번호" value={loginForm.password}
                               onChange={e => setLoginForm({ ...loginForm, password: REDACTED })}
                               style={inputStyle} />

                        <button onClick={handleLogin}
                                style={{ padding: 14, background: 'var(--primary)', color: 'white', border: 'none', borderRadius: 8, fontWeight: 'bold', cursor: 'pointer', fontSize: 15 }}>
                            로그인
                        </button>

                        <button onClick={() => setTab('signup')}
                                style={{ padding: 14, background: 'white', color: 'var(--text-dark)', border: '1px solid var(--border)', borderRadius: 8, fontWeight: 'bold', cursor: 'pointer', fontSize: 15 }}>
                            회원가입
                        </button>

                        <div style={{ textAlign: 'center', color: 'var(--text-gray)', margin: '4px 0' }}>또는</div>

                        <button onClick={handleGoogleLogin}
                                style={{ padding: 14, border: '1px solid var(--border)', borderRadius: 8, background: 'white', cursor: 'pointer', fontWeight: 'bold', fontSize: 15 }}>
                            <span style={{ color: '#4285F4' }}>G</span>  구글로 계속하기
                        </button>
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                        <input placeholder="아이디 (영문 소문자+숫자, 4~20자)" value={signupForm.username}
                               onChange={e => setSignupForm({ ...signupForm, username: e.target.value })}
                               style={inputStyle} />
                        <input placeholder="닉네임 (2~10자)" value={signupForm.nickname}
                               onChange={e => setSignupForm({ ...signupForm, nickname: e.target.value })}
                               style={inputStyle} />
                        <input type="password" placeholder="비밀번호 (8자 이상, 영문+숫자+특수문자)" value={signupForm.password}
                               onChange={e => setSignupForm({ ...signupForm, password: REDACTED })}
                               style={inputStyle} />

                        <button onClick={handleSignup}
                                style={{ padding: 14, background: 'var(--primary)', color: 'white', border: 'none', borderRadius: 8, fontWeight: 'bold', cursor: 'pointer', fontSize: 15 }}>
                            회원가입
                        </button>

                        <button onClick={() => setTab('login')}
                                style={{ padding: 10, background: 'none', border: 'none', color: 'var(--text-gray)', cursor: 'pointer', fontSize: 14 }}>
                            이미 계정이 있으신가요? 로그인
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}