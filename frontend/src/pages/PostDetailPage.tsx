import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GNB from '../components/GNB';
import Avatar from '../components/Avatar';
import EmojiText from '../components/EmojiText';
import { profilePath } from '../utils/profileNav';
import CommentSection from '../components/CommentSection';
import LikeButton from '../components/LikeButton';
import { getPost, deletePost } from '../api/postApi';
import type { PostDetail } from '../api/postApi';

export default function PostDetailPage() {
    const { postId } = useParams<{ postId: string }>();
    const navigate = useNavigate();
    const [post, setPost] = useState<PostDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);

    const nickname = localStorage.getItem('nickname');

    useEffect(() => {
        getPost(Number(postId))
            .then(setPost)
            .finally(() => setLoading(false));
    }, [postId]);

    useEffect(() => {
        if (!previewUrl) return;
        const onKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') setPreviewUrl(null);
        };
        window.addEventListener('keydown', onKeyDown);
        return () => window.removeEventListener('keydown', onKeyDown);
    }, [previewUrl]);

    const handleDelete = async () => {
        if (!confirm('게시글을 삭제할까요?')) return;
        await deletePost(Number(postId));
        navigate('/posts');
    };

    if (loading) return <div><GNB /><p style={{ textAlign: 'center', marginTop: 80 }}>불러오는 중...</p></div>;
    if (!post) return <div><GNB /><p style={{ textAlign: 'center', marginTop: 80 }}>게시글을 찾을 수 없습니다.</p></div>;

    const isAuthor = post.authorNickname === nickname;

    return (
        <div>
            <GNB />
            <div style={{ maxWidth: 800, margin: '0 auto', padding: '32px 16px' }}>
                <button onClick={() => navigate('/posts')} style={{
                    background: 'none', border: 'none', cursor: 'pointer', color: '#999', marginBottom: 16, fontSize: 14,
                }}>← 목록으로</button>

                {/* 헤더 */}
                <div style={{ borderBottom: '1px solid #eee', paddingBottom: 20, marginBottom: 24 }}>
                    <span style={{
                        fontSize: 12, padding: '2px 10px', borderRadius: 10, marginBottom: 12, display: 'inline-block',
                        background: post.category === 'QNA' ? '#E3F2FD' : '#FFF3E0',
                        color: post.category === 'QNA' ? '#1565C0' : '#E65100',
                    }}>
                        {post.category === 'QNA' ? 'Q&A' : '자랑'}
                    </span>
                    <h1 style={{ margin: '8px 0', fontSize: 24 }}>{post.title}</h1>
                    <div style={{ color: '#999', fontSize: 14, display: 'flex', gap: 16, alignItems: 'center' }}>
                        <span
                            onClick={() => navigate(profilePath(post.authorId, post.authorNickname))}
                            style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
                        >
                            <Avatar imageUrl={post.authorProfileImageUrl} nickname={post.authorNickname} size={22} />
                            {post.authorNickname}
                        </span>
                        <span>{new Date(post.createdAt).toLocaleDateString()}</span>
                        <span>조회 {post.viewCount}</span>
                        <LikeButton targetType="post" targetId={Number(postId)} />
                    </div>
                </div>

                {/* 본문 */}
                <div style={{ lineHeight: 1.8, fontSize: 16, marginBottom: 24, whiteSpace: 'pre-wrap' }}>
                    <EmojiText text={post.content} emojis={post.emojis} size={60} />
                </div>

                {/* 이미지 */}
                {post.imageUrls?.length > 0 && (
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 24 }}>
                        {post.imageUrls.map((url, i) => (
                            <img
                                key={i}
                                src={url}
                                alt=""
                                onClick={() => setPreviewUrl(url)}
                                style={{ width: 200, height: 200, objectFit: 'cover', borderRadius: 8, cursor: 'zoom-in' }}
                            />
                        ))}
                    </div>
                )}

                {/* 작성자 버튼 */}
                {isAuthor && (
                    <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginBottom: 32 }}>
                        <button onClick={() => navigate(`/posts/${postId}/edit`)} style={{
                            padding: '8px 20px', borderRadius: 8, border: '1px solid #ddd',
                            background: '#fff', cursor: 'pointer',
                        }}>수정</button>
                        <button onClick={handleDelete} style={{
                            padding: '8px 20px', borderRadius: 8, border: 'none',
                            background: '#ff4d4f', color: '#fff', cursor: 'pointer',
                        }}>삭제</button>
                    </div>
                )}

                <CommentSection postId={Number(postId)} />
            </div>

            {/* 이미지 확대 모달 */}
            {previewUrl && (
                <div
                    onClick={() => setPreviewUrl(null)}
                    style={{
                        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.85)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        zIndex: 1000, cursor: 'zoom-out', padding: 24, boxSizing: 'border-box',
                    }}
                >
                    <img
                        src={previewUrl}
                        alt=""
                        style={{ maxWidth: '90vw', maxHeight: '90vh', objectFit: 'contain', borderRadius: 4 }}
                    />
                    <button
                        onClick={() => setPreviewUrl(null)}
                        style={{
                            position: 'fixed', top: 20, right: 24, width: 40, height: 40, borderRadius: '50%',
                            border: 'none', background: 'rgba(255,255,255,0.15)', color: '#fff',
                            fontSize: 22, cursor: 'pointer', lineHeight: 1,
                        }}
                    >×</button>
                </div>
            )}
        </div>
    );
}
