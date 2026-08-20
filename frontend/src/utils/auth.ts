export function getRoleFromToken(): string | null {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    try {
        const payload = token.split('.')[1];
        return JSON.parse(atob(payload)).role;
    } catch {
        return null;
    }
}

export function getUserIdFromToken(): string | null {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    try {
        const payload = token.split('.')[1];
        // 백엔드 JwtProvider가 sub 클레임에 userId를 그대로 넣음
        return JSON.parse(atob(payload)).sub ?? null;
    } catch {
        return null;
    }
}

export function isAuthenticated(): boolean {
    const token = localStorage.getItem('accessToken');
    if (!token) return false;
    try {
        JSON.parse(atob(token.split('.')[1]));
        return true;
    } catch {
        return false;
    }
}
