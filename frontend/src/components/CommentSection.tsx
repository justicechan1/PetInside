import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getComments, createComment, updateComment, deleteComment } from '../api/commentApi';
import type { CommentItem } from '../api/commentApi';
import Avatar from './Avatar';

const inputStyle = {
    width: '100%', padding: '10px 14px', borderRadius: 8, border: '1px solid #ddd',
    fontSize: 14, boxSizing: 'border-box' as const, resize: 'vertical' as const,
};

const buttonStyle = {
    padding: '8px 16px', borderRadius: 8, border: 'none',
    background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 13,
};

const linkButtonStyle = {
    background: 'none', border: 'none', cursor: 'pointer', color: '#999', fontSize: 13, padding: 0,
};

export default function CommentSection({ postId }: { postId: number }) {
    const navigate = useNavigate();
    const nickname = localStorage.getItem('nickname');
    const isLoggedIn = !!localStorage.getItem('accessToken');

    const [comments, setComments] = useState<CommentItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);
    const [newContent, setNewContent] = useState('');
    const [replyTargetId, setReplyTargetId] = useState<number | null>(null);
    const [replyContent, setReplyContent] = useState('');
    const [editTargetId, setEditTargetId] = useState<number | null>(null);
    const [editContent, setEditContent] = useState('');

    const loadComments = () => {
        setLoading(true);
        setError(false);
        getComments(postId)
            .then(setComments)
            .catch(() => setError(true))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        loadComments();
    }, [postId]);

    const handleCreate = async () => {
        if (!newContent.trim()) return;
        try {
            await createComment(postId, { content: newContent });
            setNewContent('');
            loadComments();
        } catch (e: any) {
            alert(e.response?.data?.message ?? '댓글 등록에 실패했습니다.');
        }
    };

    const handleReplySubmit = async (parentId: number) => {
        if (!replyContent.trim()) return;
        try {
            await createComment(postId, { content: replyContent, parentId });
            setReplyContent('');
            setReplyTargetId(null);
            loadComments();
        } catch (e: any) {
            alert(e.response?.data?.message ?? '답글 등록에 실패했습니다.');
        }
    };

    const handleEditSubmit = async (commentId: number) => {
        if (!editContent.trim()) return;
        try {
            await updateComment(commentId, { content: editContent });
            setEditTargetId(null);
            loadComments();
        } catch (e: any) {
            alert(e.response?.data?.message ?? '댓글 수정에 실패했습니다.');
        }
    };

    const handleDelete = async (commentId: number) => {
        if (!confirm('댓글을 삭제할까요?')) return;
        try {
            await deleteComment(commentId);
            loadComments();
        } catch (e: any) {
            alert(e.response?.data?.message ?? '댓글 삭제에 실패했습니다.');
        }
    };

    const renderComment = (comment: CommentItem, isReply: boolean) => {
        const isAuthor = comment.authorNickname === nickname;
        const isEditing = editTargetId === comment.id;

        return (
            <div key={comment.id} style={{ marginLeft: isReply ? 32 : 0, padding: '12px 0', borderBottom: '1px solid #f0f0f0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                    <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                        <Avatar imageUrl={comment.authorProfileImageUrl} nickname={comment.authorNickname} size={20} />
                        <span style={{ fontWeight: 600, fontSize: 13 }}>{comment.authorNickname}</span>
                        <span style={{ color: '#aaa', fontSize: 12 }}>{new Date(comment.createdAt).toLocaleString()}</span>
                    </div>
                    {isAuthor && !isEditing && (
                        <div style={{ display: 'flex', gap: 10 }}>
                            <button style={linkButtonStyle} onClick={() => { setEditTargetId(comment.id); setEditContent(comment.content); }}>수정</button>
                            <button style={linkButtonStyle} onClick={() => handleDelete(comment.id)}>삭제</button>
                        </div>
                    )}
                </div>

                {isEditing ? (
                    <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                        <textarea rows={2} style={inputStyle} value={editContent} onChange={e => setEditContent(e.target.value)} />
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                            <button style={buttonStyle} onClick={() => handleEditSubmit(comment.id)}>저장</button>
                            <button style={{ ...buttonStyle, background: '#fff', color: '#333', border: '1px solid #ddd' }}
                                    onClick={() => setEditTargetId(null)}>취소</button>
                        </div>
                    </div>
                ) : (
                    <p style={{ margin: 0, fontSize: 14, lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>{comment.content}</p>
                )}

                {!isReply && (
                    <button style={{ ...linkButtonStyle, marginTop: 6 }}
                            onClick={() => { setReplyTargetId(replyTargetId === comment.id ? null : comment.id); setReplyContent(''); }}>
                        답글
                    </button>
                )}

                {replyTargetId === comment.id && (
                    <div style={{ display: 'flex', gap: 8, marginTop: 8, marginLeft: 32 }}>
                        <textarea rows={2} style={inputStyle} placeholder="답글을 입력하세요"
                                  value={replyContent} onChange={e => setReplyContent(e.target.value)} />
                        <button style={buttonStyle} onClick={() => handleReplySubmit(comment.id)}>등록</button>
                    </div>
                )}

                {comment.children && comment.children.length > 0 && (
                    <div style={{ marginTop: 8 }}>
                        {comment.children.map(child => renderComment(child, true))}
                    </div>
                )}
            </div>
        );
    };

    return (
        <div style={{ marginTop: 32, paddingTop: 24, borderTop: '1px solid #eee' }}>
            <h3 style={{ fontSize: 16, marginBottom: 16 }}>댓글 {comments.reduce((acc, c) => acc + 1 + (c.children?.length ?? 0), 0)}</h3>

            {isLoggedIn ? (
                <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                    <textarea rows={2} style={inputStyle} placeholder="댓글을 입력하세요"
                              value={newContent} onChange={e => setNewContent(e.target.value)} />
                    <button style={buttonStyle} onClick={handleCreate}>등록</button>
                </div>
            ) : (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: 14, background: '#fafafa', borderRadius: 8, marginBottom: 24 }}>
                    <span style={{ fontSize: 13, color: '#999' }}>로그인 후 댓글을 작성할 수 있습니다.</span>
                    <button style={buttonStyle} onClick={() => navigate('/login')}>로그인</button>
                </div>
            )}

            {loading ? (
                <p style={{ color: '#999', fontSize: 14 }}>댓글을 불러오는 중...</p>
            ) : error ? (
                <p style={{ color: '#e03131', fontSize: 14 }}>댓글을 불러오지 못했습니다. 게시글이 삭제되었을 수 있습니다.</p>
            ) : comments.length === 0 ? (
                <p style={{ color: '#999', fontSize: 14 }}>아직 댓글이 없습니다.</p>
            ) : (
                comments.map(comment => renderComment(comment, false))
            )}
        </div>
    );
}
