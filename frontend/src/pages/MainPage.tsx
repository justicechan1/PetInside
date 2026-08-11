import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GNB from '../components/GNB';
import axiosInstance from '../api/axiosInstance';

interface PostSummary {
    id: number;
    title: string;
    authorNickname: string;
    viewCount: number;
    createdAt: string;
}

function PopularList({ title, category, navigate }: { title: string; category: string; navigate: (path: string) => void }) {
    const [posts, setPosts] = useState<PostSummary[]>([]);

    useEffect(() => {
        axiosInstance
            .get('/api/v1/posts', { params: { category, sort: 'viewCount,desc', size: 5 } })
            .then(r => setPosts(r.data.data.content))
            .catch(() => {});
    }, [category]);

    return (
        <div style={{ flex: 1, background: '#fff', borderRadius: 12, border: '1px solid #f0f0f0', padding: '24px 28px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
            <div style={{ fontWeight: 700, fontSize: 16, color: '#222', marginBottom: 16, paddingBottom: 12, borderBottom: '2px solid var(--primary, #FF8C00)' }}>
                {title}
            </div>
            {posts.length === 0 ? (
                <p style={{ color: '#bbb', fontSize: 14, textAlign: 'center', marginTop: 20 }}>게시글이 없습니다.</p>
            ) : (
                <ol style={{ margin: 0, padding: '0 0 0 20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {posts.map(post => (
                        <li key={post.id} onClick={() => navigate(`/posts/${post.id}`)}
                            style={{ cursor: 'pointer' }}>
                            <div style={{ fontWeight: 600, fontSize: 14, color: '#222', marginBottom: 2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                {post.title}
                            </div>
                            <div style={{ fontSize: 12, color: '#999' }}>
                                {post.authorNickname} · 조회 {post.viewCount}
                            </div>
                        </li>
                    ))}
                </ol>
            )}
            <div style={{ marginTop: 16, textAlign: 'right' }}>
                <span onClick={() => navigate(`/posts?category=${category}`)}
                    style={{ fontSize: 13, color: 'var(--primary, #FF8C00)', cursor: 'pointer', fontWeight: 600 }}>
                    더보기 →
                </span>
            </div>
        </div>
    );
}

export default function MainPage() {
    const navigate = useNavigate();

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 960, margin: '0 auto', padding: '40px 16px' }}>

                {/* 배너 */}
                <div style={{
                    background: 'linear-gradient(135deg, #FF8C00 0%, #ffb347 100%)',
                    padding: '60px 40px', borderRadius: 16, textAlign: 'center', marginBottom: 40,
                    color: '#fff',
                }}>
                    <h1 style={{ margin: '0 0 10px 0', fontSize: 28 }}>우리 아이를 위한 모든 지식</h1>
                    <p style={{ margin: 0, fontSize: 16, opacity: 0.9 }}>집사들의 생생한 꿀팁과 귀여운 일상을 공유하세요!</p>
                </div>

                {/* 인기 게시글 */}
                <div style={{ display: 'flex', gap: 20 }}>
                    <PopularList title="오늘의 인기 Q&A" category="QNA" navigate={navigate} />
                    <PopularList title="화제의 자랑글" category="BOAST" navigate={navigate} />
                </div>
            </div>
        </div>
    );
}
