import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GNB from '../../../shared/components/GNB';
import EmojiPicker from '../components/EmojiPicker';
import { createPost, updatePost, getPost } from '../api/postApi';
import { uploadImage } from '../../../shared/api/imageApi';
import { isAuthenticated } from '../../../shared/utils/auth';
import { extractEmojiIds, withLegacyEmojiTokens } from '../../../shared/utils/emojiText';

const MAX_IMAGES = 5;

export default function PostFormPage() {
    const { postId } = useParams<{ postId: string }>();
    const navigate = useNavigate();
    const isEdit = !!postId;

    // URL을 직접 입력해서 들어오는 경우까지 막기 위한 방어(글쓰기 버튼 자체는 목록 페이지에서 이미 막음)
    useEffect(() => {
        if (!isAuthenticated()) {
            alert('로그인 후 이용할 수 있습니다.');
            navigate('/login');
        }
    }, [navigate]);

    const [category, setCategory] = useState('QNA');
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [imageUrls, setImageUrls] = useState<string[]>([]);
    const [uploading, setUploading] = useState(false);
    const [loading, setLoading] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const contentRef = useRef<HTMLTextAreaElement>(null);

    useEffect(() => {
        if (!isEdit) return;
        getPost(Number(postId)).then(post => {
            setCategory(post.category);
            setTitle(post.title);
            setContent(withLegacyEmojiTokens(post.content, post.emojis ?? []));
            setImageUrls(post.imageUrls ?? []);
        });
    }, [postId]);

    const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(e.target.files ?? []);
        e.target.value = ''; // 같은 파일 연속 선택 가능하도록 초기화
        if (files.length === 0) return;

        if (imageUrls.length + files.length > MAX_IMAGES) {
            alert(`이미지는 최대 ${MAX_IMAGES}장까지 첨부할 수 있습니다.`);
            return;
        }

        setUploading(true);
        try {
            const uploaded = await Promise.all(files.map(uploadImage));
            setImageUrls(prev => [...prev, ...uploaded]);
        } catch {
            alert('이미지 업로드에 실패했습니다.');
        } finally {
            setUploading(false);
        }
    };

    const handleRemoveImage = (url: string) => {
        setImageUrls(prev => prev.filter(u => u !== url));
    };

    const handleSubmit = async () => {
        if (!title.trim() || !content.trim()) {
            alert('제목과 내용을 입력해주세요.');
            return;
        }
        setLoading(true);
        try {
            const emojiIds = extractEmojiIds(content);
            if (isEdit) {
                await updatePost(Number(postId), { category, title, content, imageUrls, emojiIds });
                navigate(`/posts/${postId}`);
            } else {
                const res = await createPost({ category, title, content, imageUrls, emojiIds });
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
                    ref={contentRef}
                    value={content}
                    onChange={e => setContent(e.target.value)}
                    placeholder="내용을 입력하세요"
                    rows={15}
                    style={{ width: '100%', padding: '12px 16px', borderRadius: 8, border: '1px solid #ddd', fontSize: 15, resize: 'vertical', boxSizing: 'border-box' }}
                />
                <div style={{ marginTop: 8 }}>
                    <EmojiPicker textareaRef={contentRef} value={content} onChange={setContent} />
                </div>

                {/* 이미지 */}
                <div style={{ marginTop: 16 }}>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 8 }}>
                        {imageUrls.map(url => (
                            <div key={url} style={{ position: 'relative', width: 100, height: 100 }}>
                                <img src={url} alt="" style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 8 }} />
                                <button onClick={() => handleRemoveImage(url)} style={{
                                    position: 'absolute', top: -8, right: -8, width: 22, height: 22, borderRadius: '50%',
                                    border: 'none', background: '#333', color: '#fff', cursor: 'pointer', lineHeight: 1,
                                }}>×</button>
                            </div>
                        ))}
                        {imageUrls.length < MAX_IMAGES && (
                            <button
                                onClick={() => fileInputRef.current?.click()}
                                disabled={uploading}
                                style={{
                                    width: 100, height: 100, borderRadius: 8, border: '1px dashed #ccc',
                                    background: '#fafafa', cursor: 'pointer', fontSize: 13, color: '#999',
                                }}
                            >
                                {uploading ? '업로드 중...' : '+ 이미지 추가'}
                            </button>
                        )}
                    </div>
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/jpeg,image/png,image/gif,image/webp"
                        multiple
                        onChange={handleFileSelect}
                        style={{ display: 'none' }}
                    />
                </div>

                {/* 버튼 */}
                <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
                    <button onClick={() => navigate(-1)} style={{
                        padding: '10px 24px', borderRadius: 8, border: '1px solid #ddd', background: '#fff', cursor: 'pointer',
                    }}>취소</button>
                    <button onClick={handleSubmit} disabled={loading || uploading} style={{
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
