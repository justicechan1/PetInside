import axios from 'axios';

export const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

// refreshToken은 HttpOnly 쿠키로만 전달되므로 요청마다 쿠키를 실어 보내야 함
const axiosInstance = axios.create({ baseURL, withCredentials: true });

axiosInstance.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

const clearAuthAndRedirect = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('nickname');
    localStorage.removeItem('profileImageUrl');
    window.location.href = '/login';
};

// accessToken 만료(401) 시 refreshToken(쿠키)으로 재발급받아 원래 요청을 한 번만 재시도
let refreshPromise: Promise<string> | null = null;

const reissueAccessToken = async () => {
    // axiosInstance가 아닌 axios를 직접 써서 요청 인터셉터(Authorization 헤더) 개입을 피함
    // refreshToken은 바디가 아니라 HttpOnly 쿠키로 자동 전송되므로 withCredentials만 켜면 됨
    const res = await axios.post(`${baseURL}/api/v1/auth/reissue`, null, { withCredentials: true });
    const { accessToken } = res.data.data;
    localStorage.setItem('accessToken', accessToken);
    return accessToken;
};

axiosInstance.interceptors.response.use(
    (response) => response,
    async (error) => {
        const { config, response } = error;
        const isAuthEndpoint = config?.url?.includes('/api/v1/auth/');
        // 원래 로그인 상태가 아니었으면(토큰 없음) 세션이 "만료"된 게 아니므로 재발급/리다이렉트 대상이 아님.
        // 비회원이 보는 공개 페이지에서 부가 API 하나가 401나도 전체 화면을 로그인으로 날리지 않기 위함.
        const wasLoggedIn = !!localStorage.getItem('accessToken');

        if (response?.status !== 401 || isAuthEndpoint || config._retry || !wasLoggedIn) {
            return Promise.reject(error);
        }
        config._retry = true;

        try {
            // 동시에 여러 요청이 401을 받아도 재발급은 한 번만 수행
            refreshPromise ??= reissueAccessToken().finally(() => { refreshPromise = null; });
            const accessToken = await refreshPromise;

            config.headers.Authorization = `Bearer ${accessToken}`;
            return axiosInstance(config);
        } catch {
            clearAuthAndRedirect();
            return Promise.reject(error);
        }
    }
);

export default axiosInstance;
