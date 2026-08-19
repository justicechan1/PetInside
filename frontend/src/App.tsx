import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainPage from './pages/MainPage';
import AuthPage from './pages/AuthPage';
import OAuthCallbackPage from './pages/OAuthCallbackPage';
import PostListPage from './pages/PostListPage';
import PostDetailPage from './pages/PostDetailPage';
import PostFormPage from './pages/PostFormPage';
import MyPage from './pages/MyPage';
import SubscriptionPage from './pages/SubscriptionPage';
import SubscriptionRedirectPage from './pages/SubscriptionRedirectPage';
import AdminPage from './pages/AdminPage';
import ProtectedAdminRoute from "./components/ProtectedAdminRoute.tsx";

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
                <Route path="/subscription" element={<SubscriptionPage />} />
                <Route path="/subscription/redirect" element={<SubscriptionRedirectPage />} />
                <Route path="/admin" element={<ProtectedAdminRoute><AdminPage /></ProtectedAdminRoute>}/>
            </Routes>
        </BrowserRouter>
    );
}
