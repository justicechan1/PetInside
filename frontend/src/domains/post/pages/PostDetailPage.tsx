import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GNB from '../../../shared/components/GNB';
import Avatar from '../../../shared/components/Avatar';
import EmojiText from '../components/EmojiText';
import VerifiedBadge from '../../../shared/components/VerifiedBadge';
import { profilePath } from '../../../shared/utils/profileNav';
import CommentSection from '../components/CommentSection';
import LikeButton from '../../../shared/components/LikeButton';
import { getPost, deletePost } from '../api/postApi';
import type { PostDetail } from '../api/postApi';
import { adminDeletePost } from '../../admin/api/adminApi';
import { getRoleFromToken } from '../../../shared/utils/auth';
import { createReport } from '../api/reportApi';
import type { ReportReason } from '../api/reportApi';

export default function PostDetailPage() {
    const { postId } = useParams<{ postId: string }>();
    const navigate = useNavigate();
    const [post, setPost] = useState<PostDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [showReportForm, setShowReportForm] = useState(false);
    const [reportReason, setReportReason] = useState<ReportReason>('SPAM');
    const [reportDetail, setReportDetail] = useState('');

    const nickname = localStorage.getItem('nickname');
    const role = getRoleFromToken();
    const isLoggedIn = !!localStorage.getItem('accessToken');

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

    const handleAdminDeletePost = async (id: number) => {
        if (!confirm('관리자 권한으로 게시글을 삭제할까요?')) return;
        await adminDeletePost(id);
        navigate('/posts');
    };

    const handleReportSubmit = async () => {
        try {
            await createReport('POST', Number(postId), reportReason, reportDetail.trim() || undefined);
            alert('신고가 접수되었습니다.');
            setShowReportForm(false);
            setReportReason('SPAM');
            setReportDetail('');
        } catch (e: any) {
            alert(e.response?.data?.message ?? '신고 접수에 실패했습니다.');
        }
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
                            {post.authorVerified && <VerifiedBadge size={26} />}
                        </span>
                        <span>{new Date(post.createdAt).toLocaleDateString()}</span>
                        <span>조회 {post.viewCount}</span>
                        <LikeButton targetType="post" targetId={Number(postId)} />
                    </div>
                </div>

                {/* 본문 */}
                <div style={{ lineHeight: 1.8, fontSize: 16, marginBottom: 24, whiteSpace: 'pre-wrap' }}>
                    <EmojiText text={post.content} emojis={post.emojis} size={180} />
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
                {(isAuthor || role === 'ADMIN' || (isLoggedIn && !isAuthor)) && (
                    <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginBottom: 8 }}>
                        {isAuthor && (
                            <>
                                <button onClick={() => navigate(`/posts/${postId}/edit`)} style={{
                                    padding: '8px 20px', borderRadius: 8, border: '1px solid #ddd',
                                    background: '#fff', cursor: 'pointer',
                                }}>{role === 'ADMIN' ? '관리자 수정' : '수정'}</button>
                                <button onClick={handleDelete} style={{
                                    padding: '8px 20px', borderRadius: 8, border: 'none',
                                    background: '#ff4d4f', color: '#fff', cursor: 'pointer',
                                }}>{role === 'ADMIN' ? '관리자 삭제' : '삭제'}</button>
                            </>
                        )}
                        {role === 'ADMIN' && !isAuthor && (
                            <button onClick={() => handleAdminDeletePost(post.id)} style={{
                                padding: '8px 20px', borderRadius: 8, border: 'none',
                                background: '#ff4d4f', color: '#fff', cursor: 'pointer',
                            }}>관리자 삭제</button>
                        )}
                        {isLoggedIn && !isAuthor && (
                            <button onClick={() => setShowReportForm(v => !v)} style={{
                                padding: '8px 20px', borderRadius: 8, border: '1px solid #ddd',
                                background: '#fff', cursor: 'pointer',
                            }}>신고</button>
                        )}
                    </div>
                )}

                {showReportForm && (
                    <div style={{ padding: 12, background: '#fafafa', borderRadius: 8, marginBottom: 32 }}>
                        <div style={{ marginBottom: 8 }}>
                            <select value={reportReason} onChange={e => setReportReason(e.target.value as ReportReason)}
                                    style={{ padding: '6px 10px', borderRadius: 6, border: '1px solid #ddd', fontSize: 13 }}>
                                <option value="SPAM">스팸/광고</option>
                                <option value="ABUSE">욕설/비방</option>
                                <option value="OBSCENE">음란물</option>
                                <option value="OTHER">기타</option>
                            </select>
                        </div>
                        <textarea rows={2} placeholder="상세 사유(선택)" value={reportDetail}
                                  onChange={e => setReportDetail(e.target.value)}
                                  style={{ width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid #ddd', fontSize: 14, boxSizing: 'border-box', resize: 'vertical' }} />
                        <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                            <button onClick={handleReportSubmit} style={{
                                padding: '8px 16px', borderRadius: 8, border: 'none',
                                background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 13,
                            }}>신고 제출</button>
                            <button onClick={() => setShowReportForm(false)} style={{
                                padding: '8px 16px', borderRadius: 8, border: '1px solid #ddd',
                                background: '#fff', color: '#333', cursor: 'pointer', fontWeight: 600, fontSize: 13,
                            }}>취소</button>
                        </div>
                    </div>
                )}

                {!showReportForm && <div style={{ marginBottom: 24 }} />}

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
