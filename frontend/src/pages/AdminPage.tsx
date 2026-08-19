import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GNB from '../components/GNB';
import { getDailyStats, getUsers, updateRole, adminDeletePost, adminDeleteComment } from '../api/adminApi';
import type { DailyStats, AdminUser } from '../api/adminApi';

type Tab = 'dashboard' | 'users' | 'content';

export default function AdminPage() {
    const navigate = useNavigate();
    const [tab, setTab] = useState<Tab>('dashboard');

    // 대시보드
    const [stats, setStats] = useState<DailyStats | null>(null);

    // 회원 관리
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);

    // 게시글/댓글 삭제
    const [deletePostId, setDeletePostId] = useState('');
    const [deleteCommentId, setDeleteCommentId] = useState('');

    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        if (!token) { navigate('/login'); return; }
    }, []);

    useEffect(() => {
        if (tab === 'dashboard') {
            getDailyStats().then(setStats).catch(() => alert('통계 조회 실패. 관리자 권한을 확인하세요.'));
        }
        if (tab === 'users') {
            loadUsers(0);
        }
    }, [tab]);

    const loadUsers = (p: number) => {
        getUsers(p).then(data => {
            setUsers(data.content);
            setTotalPages(data.totalPages);
            setPage(p);
        });
    };

    const handleRoleChange = async (user: AdminUser) => {
        const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
        if (!confirm(`${user.nickname}의 권한을 ${newRole}로 변경할까요?`)) return;
        await updateRole(user.id, newRole);
        loadUsers(page);
    };

    const handleDeletePost = async () => {
        if (!deletePostId) return;
        if (!confirm(`게시글 ID ${deletePostId}를 삭제할까요?`)) return;
        await adminDeletePost(Number(deletePostId));
        alert('삭제되었습니다.');
        setDeletePostId('');
    };

    const handleDeleteComment = async () => {
        if (!deleteCommentId) return;
        if (!confirm(`댓글 ID ${deleteCommentId}를 삭제할까요?`)) return;
        await adminDeleteComment(Number(deleteCommentId));
        alert('삭제되었습니다.');
        setDeleteCommentId('');
    };

    const tabBtn = (t: Tab, label: string) => (
        <button onClick={() => setTab(t)} style={{
            padding: '10px 28px', border: 'none', borderRadius: 20, cursor: 'pointer', fontWeight: 600,
            background: tab === t ? '#1a1a2e' : '#f0f0f0',
            color: tab === t ? '#fff' : '#333',
        }}>{label}</button>
    );

    const card = (label: string, value: number | string) => (
        <div style={{
            flex: 1, background: '#fff', borderRadius: 12, padding: '28px 24px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.07)', textAlign: 'center',
        }}>
            <div style={{ fontSize: 13, color: '#999', marginBottom: 8 }}>{label}</div>
            <div style={{ fontSize: 36, fontWeight: 800, color: '#1a1a2e' }}>{value}</div>
        </div>
    );

    const inputStyle: React.CSSProperties = {
        padding: '10px 14px', border: '1px solid #ddd', borderRadius: 8,
        fontSize: 14, width: 180,
    };


    return (
        <div>
            <GNB />
            <div style={{ maxWidth: 900, margin: '0 auto', padding: '32px 16px' }}>
                <h2 style={{ marginBottom: 8 }}>관리자 페이지</h2>
                <p style={{ color: '#999', fontSize: 14, marginBottom: 28 }}>ADMIN 권한 전용</p>

                {/* 탭 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 32 }}>
                    {tabBtn('dashboard', '대시보드')}
                    {tabBtn('users', '회원 관리')}
                    {tabBtn('content', '콘텐츠 삭제')}
                </div>

                {/* 대시보드 */}
                {tab === 'dashboard' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>오늘의 통계 {stats && <span style={{ fontSize: 13, color: '#bbb', fontWeight: 400 }}>({stats.date})</span>}</h3>
                        {stats ? (
                            <div style={{ display: 'flex', gap: 16 }}>
                                {card('신규 회원', stats.newUserCount)}
                                {card('신규 게시글', stats.newPostCount)}
                                {card('활성 유저', stats.activeUserCount)}
                            </div>
                        ) : (
                            <p style={{ color: '#999' }}>불러오는 중...</p>
                        )}
                    </div>
                )}

                {/* 회원 관리 */}
                {tab === 'users' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>회원 목록</h3>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr style={{ background: '#f8f8f8' }}>
                                    {['ID', '아이디', '닉네임', '권한', '가입일', '관리'].map(h => (
                                        <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13, color: '#666', fontWeight: 600, borderBottom: '1px solid #eee' }}>{h}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {users.map(user => (
                                    <tr key={user.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                                        <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{user.id}</td>
                                        <td style={{ padding: '12px 16px', fontSize: 14 }}>{user.username}</td>
                                        <td style={{ padding: '12px 16px', fontSize: 14 }}>{user.nickname}</td>
                                        <td style={{ padding: '12px 16px' }}>
                                            <span style={{
                                                fontSize: 12, padding: '2px 10px', borderRadius: 10,
                                                background: user.role === 'ADMIN' ? '#1a1a2e' : '#f0f0f0',
                                                color: user.role === 'ADMIN' ? '#fff' : '#333',
                                            }}>{user.role}</span>
                                        </td>
                                        <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>
                                            {new Date(user.createdAt).toLocaleDateString()}
                                        </td>
                                        <td style={{ padding: '12px 16px' }}>
                                            <button onClick={() => handleRoleChange(user)} style={{
                                                padding: '5px 12px', fontSize: 12, border: '1px solid #ddd',
                                                borderRadius: 6, cursor: 'pointer', background: '#fff',
                                            }}>
                                                {user.role === 'ADMIN' ? 'USER로 변경' : 'ADMIN으로 변경'}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        {/* 페이지네이션 */}
                        {totalPages > 1 && (
                            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 24 }}>
                                {Array.from({ length: totalPages }, (_, i) => (
                                    <button key={i} onClick={() => loadUsers(i)} style={{
                                        width: 36, height: 36, borderRadius: 8, border: 'none', cursor: 'pointer',
                                        background: page === i ? '#1a1a2e' : '#f0f0f0',
                                        color: page === i ? '#fff' : '#333', fontWeight: 600,
                                    }}>{i + 1}</button>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* 콘텐츠 삭제 */}
                {tab === 'content' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
                        <section style={{ background: '#fafafa', borderRadius: 12, padding: 24 }}>
                            <h3 style={{ marginBottom: 16 }}>게시글 강제 삭제</h3>
                            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                                <input value={deletePostId} onChange={e => setDeletePostId(e.target.value)}
                                    placeholder="게시글 ID 입력" style={inputStyle} type="number" />
                                <button onClick={handleDeletePost} style={{
                                    padding: '10px 20px', border: 'none', borderRadius: 8,
                                    background: '#ff4d4f', color: '#fff', cursor: 'pointer', fontWeight: 600,
                                }}>삭제</button>
                            </div>
                        </section>

                        <section style={{ background: '#fafafa', borderRadius: 12, padding: 24 }}>
                            <h3 style={{ marginBottom: 16 }}>댓글 강제 삭제</h3>
                            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                                <input value={deleteCommentId} onChange={e => setDeleteCommentId(e.target.value)}
                                    placeholder="댓글 ID 입력" style={inputStyle} type="number" />
                                <button onClick={handleDeleteComment} style={{
                                    padding: '10px 20px', border: 'none', borderRadius: 8,
                                    background: '#ff4d4f', color: '#fff', cursor: 'pointer', fontWeight: 600,
                                }}>삭제</button>
                            </div>
                        </section>
                    </div>
                )}
            </div>
        </div>
    );
}
