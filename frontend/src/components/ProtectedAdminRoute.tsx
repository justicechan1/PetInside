// ProtectedAdminRoute.tsx (새로 만들 컴포넌트)
import { Navigate } from 'react-router-dom';

function ProtectedAdminRoute({ children }: { children: React.ReactNode }) {
    const role = localStorage.getItem('role');

    if (role !== 'ADMIN') {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
}

export default ProtectedAdminRoute;