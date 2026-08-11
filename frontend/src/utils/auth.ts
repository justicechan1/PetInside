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
