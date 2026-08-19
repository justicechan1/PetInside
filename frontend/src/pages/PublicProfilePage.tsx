import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { getPublicProfile, getPublicPosts } from '../api/mypageApi';
import type { PublicProfile, MyPost } from '../api/mypageApi';

export default function PublicProfilePage() {
    const { userId } = useParams<{ userId: string }>();
    const navigate = useNavigate();

    const [profile, setProfile] = useState<PublicProfile | null>(null);
    const [notFound, setNotFound] = useState(false);
    const [loading, setLoading] = useState(true);

    const [posts, setPosts] = useState<MyPost[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);

    useEffect(() => {
        setLoading(true);
        setNotFound(false);
        getPublicProfile(Number(userId))
            .then(setProfile)
            .catch(() => setNotFound(true))
            .finally(() => setLoading(false));
    }, [userId]);

    useEffect(() => {
        if (!userId) return;
        getPublicPosts(Number(userId), { page, size: 10 }).then(data => {
            setPosts(data.content);
            setTotalPages(data.totalPages);
        });
    }, [userId, page]);

    const cardStyle: React.CSSProperties = {
        background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)', overflow: 'hidden',
    };

    if (loading) return <div><GNB /><p style={{ textAlign: 'center', marginTop: 80 }}>불러오는 중...</p></div>;
    if (notFound || !profile) return <div><GNB /><p style={{ textAlign: 'center', marginTop: 80, color: '#999' }}>존재하지 않는 사용자입니다.</p></div>;

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 680, margin: '0 auto', padding: '32px 16px' }}>

                {/* 프로필 카드 */}
                <div style={{ ...cardStyle, marginBottom: 16 }}>
                    <div style={{ height: 80, background: 'linear-gradient(135deg, #FF8C00 0%, #ffb347 100%)' }} />
                    <div style={{ padding: '0 28px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: -44 }}>
                        <div style={{
                            width: 88, height: 88, borderRadius: '50%', border: '4px solid #fff',
                            background: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: 40, overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                        }}>
                            {profile.profileImageUrl
                                ? <img src={profile.profileImageUrl} alt="프로필" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                : '👤'}
                        </div>

                        <div style={{ marginTop: 12, textAlign: 'center' }}>
                            <div style={{ fontWeight: 700, fontSize: 20 }}>{profile.nickname}</div>
                            {profile.membershipTier && (
                                <span style={{
                                    display: 'inline-block', marginTop: 6, fontSize: 12, padding: '2px 10px',
                                    borderRadius: 10, background: '#FFF3E0', color: '#E65100', fontWeight: 600,
                                }}>
                                    {profile.membershipTier}
                                </span>
                            )}
                            {profile.badges.length > 0 && (
                                <div style={{ marginTop: 8, display: 'flex', gap: 6, justifyContent: 'center', flexWrap: 'wrap' }}>
                                    {profile.badges.map((b, i) => (
                                        <span key={i} style={{
                                            fontSize: 12, padding: '2px 8px', borderRadius: 10,
                                            background: '#E3F2FD', color: '#1565C0',
                                        }}>{b}</span>
                                    ))}
                                </div>
                            )}
                            <div style={{ color: '#999', fontSize: 13, marginTop: 8 }}>게시글 {profile.postCount}개</div>
                        </div>
                    </div>
                </div>

                {/* 작성 게시글 목록 */}
                <div style={cardStyle}>
                    <div style={{ padding: '20px 24px' }}>
                        <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 14, color: '#222' }}>작성한 게시글</div>

                        {posts.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', margin: '24px 0' }}>작성한 게시글이 없습니다.</p>
                        ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                {posts.map(post => (
                                    <div key={post.id} onClick={() => navigate(`/posts/${post.id}`)} style={{
                                        padding: '14px 18px', borderRadius: 12, border: '1px solid #f0f0f0',
                                        cursor: 'pointer', background: '#fff',
                                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                    }}>
                                        <div>
                                            <span style={{
                                                fontSize: 12, padding: '2px 8px', borderRadius: 10, marginRight: 8,
                                                background: post.category === 'QNA' ? '#E3F2FD' : '#FFF3E0',
                                                color: post.category === 'QNA' ? '#1565C0' : '#E65100',
                                            }}>
                                                {post.category === 'QNA' ? 'Q&A' : '자랑'}
                                            </span>
                                            <span style={{ fontWeight: 600 }}>{post.title}</span>
                                        </div>
                                        <span style={{ fontSize: 13, color: '#bbb', flexShrink: 0 }}>
                                            {new Date(post.createdAt).toLocaleDateString()}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}

                        {totalPages > 1 && (
                            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 20 }}>
                                {Array.from({ length: totalPages }, (_, i) => (
                                    <button key={i} onClick={() => setPage(i)} style={{
                                        width: 36, height: 36, borderRadius: 8, border: 'none', cursor: 'pointer',
                                        background: page === i ? 'var(--primary, #FF8C00)' : '#f0f0f0',
                                        color: page === i ? '#fff' : '#333', fontWeight: 600,
                                    }}>{i + 1}</button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
