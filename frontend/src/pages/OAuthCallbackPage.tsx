import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';

export default function OAuthCallbackPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    // StrictMode에서 useEffect가 두 번 실행돼도 1회용 code를 두 번 소비하지 않도록 방지
    const hasExchanged = useRef(false);

    useEffect(() => {
        if (hasExchanged.current) return;
        hasExchanged.current = true;

        const code = searchParams.get('code');
        if (!code) {
            navigate('/login');
            return;
        }

        const exchange = async () => {
            try {
                const res = await axiosInstance.post('/api/v1/auth/oauth2/exchange', { code });
                localStorage.setItem('accessToken', res.data.data.accessToken);

                const me = await axiosInstance.get('/api/v1/users/me');
                localStorage.setItem('nickname', me.data.data.nickname);

                navigate('/');
            } catch {
                alert('구글 로그인에 실패했습니다.');
                navigate('/login');
            }
        };
        exchange();
    }, [searchParams, navigate]);

    return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            로그인 처리 중입니다...
        </div>
    );
}
