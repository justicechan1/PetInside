// ProtectedAdminRoute.tsx
import { Navigate } from 'react-router-dom';

function getRoleFromToken(): string | null {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;

    try {
        const payload = token.split('.')[1];
        const decoded = JSON.parse(atob(payload));
        return decoded.role;
    } catch {
        return null;
    }
}

function ProtectedAdminRoute({ children }: { children: React.ReactNode }) {
    const role = getRoleFromToken();
    
    if (role !== 'ADMIN') {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
}

export default ProtectedAdminRoute;