import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainPage from './pages/MainPage';
import AuthPage from './pages/AuthPage';
import PostListPage from './pages/PostListPage';
import PostDetailPage from './pages/PostDetailPage';
import PostFormPage from './pages/PostFormPage';

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<MainPage />} />
                <Route path="/login" element={<AuthPage />} />
                <Route path="/posts" element={<PostListPage />} />
                <Route path="/posts/new" element={<PostFormPage />} />
                <Route path="/posts/:postId" element={<PostDetailPage />} />
                <Route path="/posts/:postId/edit" element={<PostFormPage />} />
            </Routes>
        </BrowserRouter>
    );
}
