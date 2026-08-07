import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GNB from '../components/GNB';
import { getMyInfo, updateNickname, updatePassword, getMyPosts } from '../api/mypageApi';
import type { UserInfo, MyPost } from '../api/mypageApi';

type Tab = 'profile' | 'posts';

export default function MyPage() {
    const navigate = useNavigate();
    const [tab, setTab] = useState<Tab>('profile');

    useEffect(() => {
        if (!localStorage.getItem('accessToken')) navigate('/login');
    }, []);
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

    // 닉네임 변경
    const [nickname, setNickname] = useState('');

    // 비밀번호 변경
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');

    // 내 게시글
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

    const handleNicknameUpdate = async () => {
        await updateNickname(nickname);
        alert('닉네임이 변경되었습니다.');
        localStorage.setItem('nickname', nickname);
        setUserInfo(prev => prev ? { ...prev, nickname } : prev);
    };

    const handlePasswordUpdate = async () => {
        if (!currentPassword || !newPassword) { alert('비밀번호를 입력해주세요.'); return; }
        await updatePassword(currentPassword, newPassword);
        alert('비밀번호가 변경되었습니다.');
        setCurrentPassword('');
        setNewPassword('');
    };

    const inputStyle: React.CSSProperties = {
        padding: '10px 14px', border: '1px solid #ddd', borderRadius: 8,
        fontSize: 14, width: '100%', boxSizing: 'border-box',
    };

    const tabBtnStyle = (active: boolean): React.CSSProperties => ({
        padding: '10px 28px', border: 'none', borderRadius: 20, cursor: 'pointer', fontWeight: 600,
        background: active ? 'var(--primary, #FF8C00)' : '#f0f0f0',
        color: active ? '#fff' : '#333',
    });

    return (
        <div>
            <GNB />
            <div style={{ maxWidth: 700, margin: '0 auto', padding: '32px 16px' }}>
                <h2 style={{ marginBottom: 24 }}>마이페이지</h2>

                {/* 탭 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 32 }}>
                    <button style={tabBtnStyle(tab === 'profile')} onClick={() => setTab('profile')}>프로필</button>
                    <button style={tabBtnStyle(tab === 'posts')} onClick={() => setTab('posts')}>내 게시글</button>
                </div>

                {/* 프로필 탭 */}
                {tab === 'profile' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>

                        {/* 기본 정보 */}
                        <section style={{ background: '#fafafa', borderRadius: 12, padding: 24 }}>
                            <h3 style={{ marginBottom: 16 }}>기본 정보</h3>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
                                <div style={{
                                    width: 72, height: 72, borderRadius: '50%',
                                    background: '#eee', display: 'flex', alignItems: 'center',
                                    justifyContent: 'center', fontSize: 32, overflow: 'hidden',
                                }}>
                                    {userInfo?.profileImageUrl
                                        ? <img src={userInfo.profileImageUrl} alt="프로필" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                        : '👤'}
                                </div>
                                <div>
                                    <div style={{ fontWeight: 700, fontSize: 18 }}>{userInfo?.nickname}</div>
                                    <div style={{ color: '#999', fontSize: 14 }}>{userInfo?.username}</div>
                                    <div style={{ fontSize: 12, color: '#bbb', marginTop: 4 }}>
                                        가입일: {userInfo ? new Date(userInfo.createdAt).toLocaleDateString() : '-'}
                                    </div>
                                </div>
                            </div>
                        </section>

                        {/* 닉네임 변경 */}
                        <section style={{ background: '#fafafa', borderRadius: 12, padding: 24 }}>
                            <h3 style={{ marginBottom: 16 }}>닉네임 변경</h3>
                            <div style={{ display: 'flex', gap: 8 }}>
                                <input value={nickname} onChange={e => setNickname(e.target.value)}
                                    placeholder="새 닉네임" style={{ ...inputStyle, flex: 1 }} />
                                <button onClick={handleNicknameUpdate} style={{
                                    padding: '10px 20px', border: 'none', borderRadius: 8,
                                    background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600,
                                }}>변경</button>
                            </div>
                        </section>

                        {/* 비밀번호 변경 */}
                        {!userInfo?.provider && (
                            <section style={{ background: '#fafafa', borderRadius: 12, padding: 24 }}>
                                <h3 style={{ marginBottom: 16 }}>비밀번호 변경</h3>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                    <input type="password" value={currentPassword}
                                        onChange={e => setCurrentPassword(e.target.value)}
                                        placeholder="현재 비밀번호" style={inputStyle} />
                                    <input type="password" value={newPassword}
                                        onChange={e => setNewPassword(e.target.value)}
                                        placeholder="새 비밀번호" style={inputStyle} />
                                    <button onClick={handlePasswordUpdate} style={{
                                        padding: '10px 20px', border: 'none', borderRadius: 8,
                                        background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600,
                                    }}>변경</button>
                                </div>
                            </section>
                        )}
                    </div>
                )}

                {/* 내 게시글 탭 */}
                {tab === 'posts' && (
                    <div>
                        {/* 카테고리 필터 */}
                        <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                            {[['', '전체'], ['QNA', 'Q&A'], ['BOAST', '자랑']].map(([val, label]) => (
                                <button key={val} onClick={() => { setCategoryFilter(val); setPage(0); }}
                                    style={tabBtnStyle(categoryFilter === val)}>
                                    {label}
                                </button>
                            ))}
                        </div>

                        {/* 게시글 목록 */}
                        {posts.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', marginTop: 40 }}>작성한 게시글이 없습니다.</p>
                        ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                {posts.map(post => (
                                    <div key={post.id} onClick={() => navigate(`/posts/${post.id}`)} style={{
                                        padding: '14px 18px', borderRadius: 10, border: '1px solid #eee',
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
                                        <span style={{ fontSize: 13, color: '#bbb' }}>
                                            {new Date(post.createdAt).toLocaleDateString()}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* 페이지네이션 */}
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
