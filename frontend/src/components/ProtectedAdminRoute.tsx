import { Navigate } from 'react-router-dom';
import { getRoleFromToken } from '../utils/auth';

function ProtectedAdminRoute({ children }: { children: React.ReactNode }) {
    const role = getRoleFromToken();
    
    if (role !== 'ADMIN') {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
}

export default ProtectedAdminRoute;