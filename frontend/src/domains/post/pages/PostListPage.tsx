import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import GNB from '../../../shared/components/GNB';
import Avatar from '../../../shared/components/Avatar';
import VerifiedBadge from '../../../shared/components/VerifiedBadge';
import { profilePath } from '../../../shared/utils/profileNav';
import { getPosts } from '../api/postApi';
import type { PostListItem } from '../api/postApi';
import { isAuthenticated } from '../../../shared/utils/auth';

export default function PostListPage() {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();

    const category = searchParams.get('category') ?? '';
    const keyword = searchParams.get('keyword') ?? '';
    const page = Number(searchParams.get('page') ?? 0);

    const [posts, setPosts] = useState<PostListItem[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [searchInput, setSearchInput] = useState(keyword);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        setLoading(true);
        getPosts({ category: category || undefined, keyword: keyword || undefined, page, size: 10 })
            .then(data => {
                setPosts(data.content);
                setTotalPages(data.totalPages);
            })
            .finally(() => setLoading(false));
    }, [category, keyword, page]);

    const setCategory = (cat: string) => setSearchParams({ category: cat, keyword, page: '0' });
    const handleSearch = () => setSearchParams({ category, keyword: searchInput, page: '0' });
    const setPage = (p: number) => setSearchParams({ category, keyword, page: String(p) });

    const handleWriteClick = () => {
        if (!isAuthenticated()) {
            alert('로그인 후 이용할 수 있습니다.');
            navigate('/login');
            return;
        }
        navigate('/posts/new');
    };

    return (
        <div>
            <GNB />
            <div style={{ maxWidth: 900, margin: '0 auto', padding: '32px 16px' }}>

                {/* 카테고리 탭 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                    {['', 'QNA', 'BOAST'].map(cat => (
                        <button key={cat} onClick={() => setCategory(cat)} style={{
                            padding: '8px 20px', borderRadius: 20, border: 'none', cursor: 'pointer',
                            background: category === cat ? 'var(--primary, #FF8C00)' : '#f0f0f0',
                            color: category === cat ? '#fff' : '#333', fontWeight: 600,
                        }}>
                            {cat === '' ? '전체' : cat === 'QNA' ? 'Q&A' : '자랑'}
                        </button>
                    ))}
                    <button onClick={handleWriteClick} style={{
                        marginLeft: 'auto', padding: '8px 20px', borderRadius: 20,
                        border: 'none', background: 'var(--primary, #FF8C00)', color: '#fff',
                        cursor: 'pointer', fontWeight: 600,
                    }}>
                        글쓰기
                    </button>
                </div>

                {/* 검색 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                    <input
                        value={searchInput}
                        onChange={e => setSearchInput(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleSearch()}
                        placeholder="제목으로 검색"
                        style={{ flex: 1, padding: '10px 16px', borderRadius: 8, border: '1px solid #ddd', fontSize: 14 }}
                    />
                    <button onClick={handleSearch} style={{
                        padding: '10px 20px', borderRadius: 8, border: 'none',
                        background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer',
                    }}>검색</button>
                </div>

                {/* 목록 */}
                {loading ? (
                    <p style={{ textAlign: 'center', color: '#999' }}>불러오는 중...</p>
                ) : posts.length === 0 ? (
                    <p style={{ textAlign: 'center', color: '#999' }}>게시글이 없습니다.</p>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                        {posts.map(post => (
                            <div key={post.id} onClick={() => navigate(`/posts/${post.id}`)} style={{
                                padding: '16px 20px', borderRadius: 10, border: '1px solid #eee',
                                cursor: 'pointer', background: '#fff',
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16,
                            }}>
                                {post.thumbnailUrl ? (
                                    <img
                                        src={post.thumbnailUrl}
                                        alt=""
                                        style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 8, flexShrink: 0 }}
                                    />
                                ) : (
                                    <div style={{
                                        width: 64, height: 64, borderRadius: 8, flexShrink: 0,
                                        background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        color: '#ccc', fontSize: 22,
                                    }}>🐾</div>
                                )}
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <span style={{
                                        fontSize: 12, padding: '2px 8px', borderRadius: 10, marginRight: 8,
                                        background: post.category === 'QNA' ? '#E3F2FD' : '#FFF3E0',
                                        color: post.category === 'QNA' ? '#1565C0' : '#E65100',
                                    }}>
                                        {post.category === 'QNA' ? 'Q&A' : '자랑'}
                                    </span>
                                    <span style={{ fontWeight: 600 }}>{post.title}</span>
                                    <div style={{ fontSize: 13, color: '#999', marginTop: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
                                        <span
                                            onClick={e => { e.stopPropagation(); navigate(profilePath(post.authorId, post.authorNickname)); }}
                                            style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
                                        >
                                            <Avatar imageUrl={post.authorProfileImageUrl} nickname={post.authorNickname} size={18} />
                                            {post.authorNickname}
                                            {post.authorVerified && <VerifiedBadge />}
                                        </span>
                                        · {new Date(post.createdAt).toLocaleDateString()}
                                    </div>
                                </div>
                                <div style={{ fontSize: 13, color: '#999', textAlign: 'right', flexShrink: 0 }}>
                                    <div>조회 {post.viewCount}</div>
                                    <div>댓글 {post.commentCount}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {/* 페이지네이션 */}
                {totalPages > 1 && (
                    <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 32 }}>
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
    );
}
