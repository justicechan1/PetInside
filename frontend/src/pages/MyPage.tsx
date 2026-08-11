import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GNB from '../components/GNB';
import { getMyInfo, updateNickname, updatePassword, updateProfileImage, getMyPosts } from '../api/mypageApi';
import { uploadImage } from '../api/imageApi';
import type { UserInfo, MyPost } from '../api/mypageApi';
import { isAuthenticated } from '../utils/auth';

type Tab = 'profile' | 'posts';

export default function MyPage() {
    const navigate = useNavigate();
    const [tab, setTab] = useState<Tab>('profile');

    useEffect(() => {
        if (!isAuthenticated()) navigate('/login');
    }, [navigate]);

    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [nickname, setNickname] = useState('');
    const [nicknameError, setNicknameError] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [passwordError, setPasswordError] = useState('');

    const fileInputRef = useRef<HTMLInputElement>(null);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [imageError, setImageError] = useState('');

    const [posts, setPosts] = useState<MyPost[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);
    const [categoryFilter, setCategoryFilter] = useState('');

    useEffect(() => {
        getMyInfo().then(info => {
            setUserInfo(info);
            setNickname(info.nickname);
        });
    }, []);

    useEffect(() => {
        if (tab !== 'posts') return;
        getMyPosts({ category: categoryFilter || undefined, page, size: 10 }).then(data => {
            setPosts(data.content);
            setTotalPages(data.totalPages);
        });
    }, [tab, categoryFilter, page]);

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setSelectedFile(file);
        setPreviewUrl(URL.createObjectURL(file));
    };

    const handleProfileImageSave = async () => {
        if (!selectedFile) return;
        setImageError('');
        try {
            const url = await uploadImage(selectedFile);
            await updateProfileImage(url);
            setUserInfo(prev => prev ? { ...prev, profileImageUrl: url } : prev);
            handleProfileImageCancel();
        } catch (e: any) {
            setImageError(e.response?.data?.message ?? '프로필 이미지 변경에 실패했습니다.');
        }
    };

    const handleProfileImageCancel = () => {
        setSelectedFile(null);
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        setPreviewUrl(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    const handleNicknameUpdate = async () => {
        setNicknameError('');
        if (!nickname.trim()) { setNicknameError('닉네임을 입력해주세요.'); return; }
        if (nickname === userInfo?.nickname) { setNicknameError('현재 닉네임과 동일합니다.'); return; }
        try {
            await updateNickname(nickname);
            alert('닉네임이 변경되었습니다.');
            localStorage.setItem('nickname', nickname);
            window.dispatchEvent(new StorageEvent('storage', { key: 'nickname', newValue: nickname }));
            setUserInfo(prev => prev ? { ...prev, nickname } : prev);
        } catch (e: any) {
            setNicknameError(e.response?.data?.message ?? '닉네임 변경에 실패했습니다.');
        }
    };

    const handlePasswordUpdate = async () => {
        setPasswordError('');
        if (!currentPassword || !newPassword) {
            setPasswordError('비밀번호를 입력해주세요.');
            return;
        }
        try {
            await updatePassword(currentPassword, newPassword);
            alert('비밀번호가 변경되었습니다.');
            setCurrentPassword('');
            setNewPassword('');
        } catch (e: any) {
            setPasswordError(e.response?.data?.message ?? '비밀번호 변경에 실패했습니다.');
        }
    };

    const tabBtnStyle = (active: boolean): React.CSSProperties => ({
        padding: '10px 28px', border: 'none', borderRadius: 20, cursor: 'pointer', fontWeight: 600,
        background: active ? 'var(--primary, #FF8C00)' : '#f0f0f0',
        color: active ? '#fff' : '#333',
    });

    const inputStyle: React.CSSProperties = {
        padding: '11px 14px', border: '1px solid #e0e0e0', borderRadius: 8,
        fontSize: 14, width: '100%', boxSizing: 'border-box', background: '#fff',
    };

    const cardStyle: React.CSSProperties = {
        background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)', overflow: 'hidden',
    };

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 680, margin: '0 auto', padding: '32px 16px' }}>

                {/* 탭 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                    <button style={tabBtnStyle(tab === 'profile')} onClick={() => setTab('profile')}>프로필</button>
                    <button style={tabBtnStyle(tab === 'posts')} onClick={() => setTab('posts')}>내 게시글</button>
                </div>

                {/* 프로필 탭 */}
                {tab === 'profile' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                        {/* 프로필 카드 */}
                        <div style={cardStyle}>
                            {/* 상단 배너 */}
                            <div style={{
                                height: 80,
                                background: 'linear-gradient(135deg, #FF8C00 0%, #ffb347 100%)',
                            }} />

                            {/* 아바타 + 정보 */}
                            <div style={{ padding: '0 28px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: -44 }}>
                                <div style={{ position: 'relative' }}>
                                    <div style={{
                                        width: 88, height: 88, borderRadius: '50%',
                                        border: '4px solid #fff',
                                        background: '#eee', display: 'flex', alignItems: 'center',
                                        justifyContent: 'center', fontSize: 40, overflow: 'hidden',
                                        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                                    }}>
                                        {previewUrl
                                            ? <img src={previewUrl} alt="미리보기" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                            : userInfo?.profileImageUrl
                                                ? <img src={userInfo.profileImageUrl} alt="프로필" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                : '👤'}
                                    </div>
                                    <button
                                        onClick={() => fileInputRef.current?.click()}
                                        title="프로필 사진 변경"
                                        style={{
                                            position: 'absolute', bottom: 4, right: 4,
                                            width: 26, height: 26, borderRadius: '50%',
                                            border: '2px solid #fff',
                                            background: 'var(--primary, #FF8C00)',
                                            color: '#fff', fontSize: 11, cursor: 'pointer',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}
                                    >✏️</button>
                                    <input ref={fileInputRef} type="file" accept="image/*"
                                        onChange={handleFileSelect} style={{ display: 'none' }} />
                                </div>

                                <div style={{ marginTop: 12, textAlign: 'center' }}>
                                    <div style={{ fontWeight: 700, fontSize: 20 }}>{userInfo?.nickname}</div>
                                    <div style={{ color: '#999', fontSize: 14, marginTop: 4 }}>{userInfo?.username}</div>
                                    <div style={{ fontSize: 12, color: '#bbb', marginTop: 6 }}>
                                        가입일 {userInfo ? new Date(userInfo.createdAt).toLocaleDateString() : '-'}
                                    </div>
                                </div>

                                {selectedFile && (
                                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, marginTop: 12 }}>
                                        <div style={{ display: 'flex', gap: 8 }}>
                                            <button onClick={handleProfileImageSave} style={{
                                                padding: '8px 20px', border: 'none', borderRadius: 8,
                                                background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 13,
                                            }}>저장</button>
                                            <button onClick={handleProfileImageCancel} style={{
                                                padding: '8px 20px', border: '1px solid #ddd', borderRadius: 8,
                                                background: '#fff', cursor: 'pointer', fontSize: 13,
                                            }}>취소</button>
                                        </div>
                                        {imageError && (
                                            <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{imageError}</p>
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 닉네임 변경 카드 */}
                        <div style={cardStyle}>
                            <div style={{ padding: '20px 24px' }}>
                                <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 14, color: '#222' }}>닉네임 변경</div>
                                <div style={{ display: 'flex', gap: 8 }}>
                                    <input value={nickname}
                                        onChange={e => { setNickname(e.target.value); setNicknameError(''); }}
                                        placeholder="새 닉네임"
                                        style={{ ...inputStyle, flex: 1, borderColor: nicknameError ? '#f44336' : '#e0e0e0' }} />
                                    <button onClick={handleNicknameUpdate} style={{
                                        padding: '11px 20px', border: 'none', borderRadius: 8, whiteSpace: 'nowrap',
                                        background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 14,
                                    }}>변경</button>
                                </div>
                                {nicknameError && (
                                    <p style={{ margin: '8px 0 0', fontSize: 13, color: '#f44336' }}>{nicknameError}</p>
                                )}
                            </div>
                        </div>

                        {/* 비밀번호 변경 카드 */}
                        {!userInfo?.provider && (
                            <div style={cardStyle}>
                                <div style={{ padding: '20px 24px' }}>
                                    <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 14, color: '#222' }}>비밀번호 변경</div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                        <input type="password" value={currentPassword}
                                            onChange={e => setCurrentPassword(e.target.value)}
                                            placeholder="현재 비밀번호" style={inputStyle} />
                                        <input type="password" value={newPassword}
                                            onChange={e => { setNewPassword(e.target.value); setPasswordError(''); }}
                                            placeholder="새 비밀번호 (8자 이상, 영문+숫자+특수문자)" style={{
                                                ...inputStyle,
                                                borderColor: passwordError ? '#f44336' : '#e0e0e0',
                                            }} />
                                        {passwordError && (
                                            <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{passwordError}</p>
                                        )}
                                        <button onClick={handlePasswordUpdate} style={{
                                            padding: '11px', border: 'none', borderRadius: 8,
                                            background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 14,
                                        }}>변경</button>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* 내 게시글 탭 */}
                {tab === 'posts' && (
                    <div>
                        <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                            {[['', '전체'], ['QNA', 'Q&A'], ['BOAST', '자랑']].map(([val, label]) => (
                                <button key={val} onClick={() => { setCategoryFilter(val); setPage(0); }}
                                    style={tabBtnStyle(categoryFilter === val)}>
                                    {label}
                                </button>
                            ))}
                        </div>

                        {posts.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', marginTop: 40 }}>작성한 게시글이 없습니다.</p>
                        ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                {posts.map(post => (
                                    <div key={post.id} onClick={() => navigate(`/posts/${post.id}`)} style={{
                                        padding: '14px 18px', borderRadius: 12, border: '1px solid #f0f0f0',
                                        cursor: 'pointer', background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
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
                            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 24 }}>
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
                )}
            </div>
        </div>
    );
}
