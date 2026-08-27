import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainPage from './pages/MainPage';
import AuthPage from './domains/auth/pages/AuthPage';
import OAuthCallbackPage from './domains/auth/pages/OAuthCallbackPage';
import PostListPage from './domains/post/pages/PostListPage';
import PostDetailPage from './domains/post/pages/PostDetailPage';
import PostFormPage from './domains/post/pages/PostFormPage';
import MyPage from './domains/mypage/pages/MyPage';
import PublicProfilePage from './domains/user/pages/PublicProfilePage';
import SubscriptionPage from './domains/subscription/pages/SubscriptionPage';
import SubscriptionRedirectPage from './domains/subscription/pages/SubscriptionRedirectPage';
import PaymentHistoryPage from './domains/subscription/pages/PaymentHistoryPage';
import AdminPage from './domains/admin/pages/AdminPage';
import ProtectedAdminRoute from './domains/admin/components/ProtectedAdminRoute';

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<MainPage />} />
                <Route path="/login" element={<AuthPage />} />
                <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
                <Route path="/posts" element={<PostListPage />} />
                <Route path="/posts/new" element={<PostFormPage />} />
                <Route path="/posts/:postId" element={<PostDetailPage />} />
                <Route path="/posts/:postId/edit" element={<PostFormPage />} />
                <Route path="/mypage" element={<MyPage />} />
                <Route path="/users/:userId" element={<PublicProfilePage />} />
                <Route path="/subscription" element={<SubscriptionPage />} />
                <Route path="/subscription/redirect" element={<SubscriptionRedirectPage />} />
                <Route path="/subscription/history" element={<PaymentHistoryPage />} />
                <Route path="/admin" element={<ProtectedAdminRoute><AdminPage /></ProtectedAdminRoute>}/>
            </Routes>
        </BrowserRouter>
    );
}
