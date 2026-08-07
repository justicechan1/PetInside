import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { createPost, updatePost, getPost } from '../api/postApi';

export default function PostFormPage() {
    const { postId } = useParams<{ postId: string }>();
    const navigate = useNavigate();
    const isEdit = !!postId;

    const [category, setCategory] = useState('QNA');
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!isEdit) return;
        getPost(Number(postId)).then(post => {
            setCategory(post.category);
            setTitle(post.title);
            setContent(post.content);
        });
    }, [postId]);

    const handleSubmit = async () => {
        if (!title.trim() || !content.trim()) {
            alert('제목과 내용을 입력해주세요.');
            return;
        }
        setLoading(true);
        try {
            if (isEdit) {
                await updatePost(Number(postId), { category, title, content });
                navigate(`/posts/${postId}`);
            } else {
                const res = await createPost({ category, title, content });
                navigate(`/posts/${res.data.id}`);
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <GNB />
            <div style={{ maxWidth: 800, margin: '0 auto', padding: '32px 16px' }}>
                <h2 style={{ marginBottom: 24 }}>{isEdit ? '게시글 수정' : '게시글 작성'}</h2>

                {/* 카테고리 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                    {['QNA', 'BOAST'].map(cat => (
                        <button key={cat} onClick={() => setCategory(cat)} style={{
                            padding: '8px 24px', borderRadius: 20, border: 'none', cursor: 'pointer',
                            background: category === cat ? 'var(--primary, #FF8C00)' : '#f0f0f0',
                            color: category === cat ? '#fff' : '#333', fontWeight: 600,
                        }}>
                            {cat === 'QNA' ? 'Q&A' : '자랑'}
                        </button>
                    ))}
                </div>

                {/* 제목 */}
                <input
                    value={title}
                    onChange={e => setTitle(e.target.value)}
                    placeholder="제목을 입력하세요"
                    style={{ width: '100%', padding: '12px 16px', borderRadius: 8, border: '1px solid #ddd', fontSize: 16, marginBottom: 12, boxSizing: 'border-box' }}
                />

                {/* 내용 */}
                <textarea
                    value={content}
                    onChange={e => setContent(e.target.value)}
                    placeholder="내용을 입력하세요"
                    rows={15}
                    style={{ width: '100%', padding: '12px 16px', borderRadius: 8, border: '1px solid #ddd', fontSize: 15, resize: 'vertical', boxSizing: 'border-box' }}
                />

                {/* 버튼 */}
                <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
                    <button onClick={() => navigate(-1)} style={{
                        padding: '10px 24px', borderRadius: 8, border: '1px solid #ddd', background: '#fff', cursor: 'pointer',
                    }}>취소</button>
                    <button onClick={handleSubmit} disabled={loading} style={{
                        padding: '10px 24px', borderRadius: 8, border: 'none',
                        background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600,
                    }}>
                        {loading ? '처리 중...' : isEdit ? '수정 완료' : '등록'}
                    </button>
                </div>
            </div>
        </div>
    );
}
