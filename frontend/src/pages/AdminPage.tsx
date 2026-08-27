import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GNB from '../components/GNB';
import {
    getDailyStats, getUsers, updateRole,
    deleteUser, getSubscriptions, getPayments, getAllPosts,
    getReports, resolveReport
} from '../api/adminApi';
import type { DailyStats, AdminUser, Subscription, Payment, SubscriptionStatus, PaymentStatus, AdminPost, AdminReport, ReportStatus } from '../api/adminApi';

type Tab = 'dashboard' | 'users' | 'content' | 'subscription' | 'payment' | 'report';

export default function AdminPage() {
    const navigate = useNavigate();
    const [tab, setTab] = useState<Tab>('dashboard');
    const [isLoading, setIsLoading] = useState(false);

    // 콘텐츠 삭제 (게시글 목록)
    const [posts, setPosts] = useState<AdminPost[]>([]);
    const [postTotalPages, setPostTotalPages] = useState(0);
    const [postPage, setPostPage] = useState(0);

    const loadPosts = (p: number) => {
        setIsLoading(true);
        getAllPosts(p)
            .then(data => { setPosts(data.content); setPostTotalPages(data.totalPages); setPostPage(p); })
            .catch(() => alert('게시글 목록 조회 실패'))
            .finally(() => setIsLoading(false));
    };

    // 대시보드
    const [stats, setStats] = useState<DailyStats | null>(null);

    // 회원 관리
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);

    // 구독 회원 관리 (F-41)
    const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
    const [subTotalPages, setSubTotalPages] = useState(0);
    const [subPage, setSubPage] = useState(0);
    const [subStatusFilter, setSubStatusFilter] = useState<SubscriptionStatus | ''>('');

    // 결제 목록 (F-42)
    const [payments, setPayments] = useState<Payment[]>([]);
    const [payTotalPages, setPayTotalPages] = useState(0);
    const [payPage, setPayPage] = useState(0);
    const [payStatusFilter, setPayStatusFilter] = useState<PaymentStatus | ''>('');
    const [payUserIdFilter, setPayUserIdFilter] = useState('');

    // 신고 관리
    const [reports, setReports] = useState<AdminReport[]>([]);
    const [reportTotalPages, setReportTotalPages] = useState(0);
    const [reportPage, setReportPage] = useState(0);
    const [reportStatusFilter, setReportStatusFilter] = useState<ReportStatus | ''>('');

    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        if (!token) { navigate('/login'); return; }
    }, [navigate]);

    useEffect(() => {
        if (tab === 'dashboard') {
            getDailyStats().then(setStats).catch(() => alert('통계 조회 실패. 관리자 권한을 확인하세요.'));
        }
        if (tab === 'users') loadUsers(0);
        if (tab === 'content') loadPosts(0);
        if (tab === 'subscription') loadSubscriptions(0, subStatusFilter);
        if (tab === 'payment') loadPayments(0, payStatusFilter, payUserIdFilter ? Number(payUserIdFilter) : undefined);
        if (tab === 'report') loadReports(0, reportStatusFilter);
    }, [tab]);

    const loadUsers = (p: number) => {
        setIsLoading(true);
        getUsers(p)
            .then(data => { setUsers(data.content); setTotalPages(data.totalPages); setPage(p); })
            .catch(() => alert('회원 목록 조회 실패'))
            .finally(() => setIsLoading(false));
    };

    const loadSubscriptions = (p: number, status?: SubscriptionStatus | '') => {
        setIsLoading(true);
        getSubscriptions(p, status)
            .then(data => { setSubscriptions(data.content); setSubTotalPages(data.totalPages); setSubPage(p); })
            .catch(() => alert('구독 목록 조회 실패'))
            .finally(() => setIsLoading(false));
    };

    const loadPayments = (p: number, status?: PaymentStatus | '', userId?: number) => {
        setIsLoading(true);
        getPayments(p, status, userId)
            .then(data => { setPayments(data.content); setPayTotalPages(data.totalPages); setPayPage(p); })
            .catch(() => alert('결제 목록 조회 실패'))
            .finally(() => setIsLoading(false));
    };

    const handleRoleChange = async (user: AdminUser) => {
        const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
        if (!confirm(`${user.nickname}의 권한을 ${newRole}로 변경할까요?`)) return;
        await updateRole(user.id, newRole);
        loadUsers(page);
    };

    const handleDeleteUser = async (user: AdminUser) => {
        if (!confirm(`${user.nickname}(${user.username}) 회원을 삭제할까요?\n작성한 게시글/댓글도 함께 삭제됩니다.`)) return;
        await deleteUser(user.id);
        alert('회원이 삭제되었습니다.');
        loadUsers(page);
    };

    const loadReports = (p: number, status?: ReportStatus | '') => {
        setIsLoading(true);
        getReports(p, status)
            .then(data => { setReports(data.content); setReportTotalPages(data.totalPages); setReportPage(p); })
            .catch(() => alert('신고 목록 조회 실패'))
            .finally(() => setIsLoading(false));
    };

    const handleResolveReport = async (report: AdminReport, action: 'DELETE' | 'REJECT') => {
        const message = action === 'DELETE'
            ? '신고된 대상을 삭제 처리할까요?'
            : '이 신고를 반려할까요?';
        if (!confirm(message)) return;
        try {
            await resolveReport(report.id, action);
            alert(action === 'DELETE' ? '삭제 처리되었습니다.' : '반려되었습니다.');
            loadReports(reportPage, reportStatusFilter);
        } catch (e: any) {
            alert(e.response?.data?.message ?? '신고 처리에 실패했습니다.');
        }
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
        padding: '10px 14px', border: '1px solid #ddd', borderRadius: 8, fontSize: 14, width: 180,
    };

    const pagination = (total: number, cur: number, onClick: (i: number) => void) => (
        total > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 24 }}>
                {Array.from({ length: total }, (_, i) => (
                    <button key={i} onClick={() => onClick(i)} style={{
                        width: 36, height: 36, borderRadius: 8, border: 'none', cursor: 'pointer',
                        background: cur === i ? '#1a1a2e' : '#f0f0f0', color: cur === i ? '#fff' : '#333', fontWeight: 600,
                    }}>{i + 1}</button>
                ))}
            </div>
        )
    );

    // @ts-ignore
    return (
        <div>
            <GNB />
            <div style={{ maxWidth: 900, margin: '0 auto', padding: '32px 16px' }}>
                <h2 style={{ marginBottom: 8 }}>관리자 페이지</h2>
                <p style={{ color: '#999', fontSize: 14, marginBottom: 28 }}>ADMIN 권한 전용</p>

                <div style={{ display: 'flex', gap: 8, marginBottom: 32, flexWrap: 'wrap' }}>
                    {tabBtn('dashboard', '대시보드')}
                    {tabBtn('users', '회원 관리')}
                    {tabBtn('content', '콘텐츠 삭제')}
                    {tabBtn('report', '신고 관리')}
                    {tabBtn('subscription', '구독 회원 관리')}
                    {tabBtn('payment', '결제 내역')}
                </div>

                {isLoading && <div style={{ textAlign: 'center', padding: '40px 0', color: '#666' }}>로딩 중...</div>}

                {/* 대시보드 */}
                {!isLoading && tab === 'dashboard' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>오늘의 통계 {stats && <span style={{ fontSize: 13, color: '#bbb', fontWeight: 400 }}>({stats.date})</span>}</h3>
                        {stats ? (
                            <div style={{ display: 'flex', gap: 16 }}>
                                {card('신규 회원', stats.newUserCount)}
                                {card('신규 게시글', stats.newPostCount)}
                                {card('활성 유저', stats.activeUserCount)}
                            </div>
                        ) : <p style={{ color: '#999' }}>통계 데이터를 불러올 수 없습니다.</p>}
                    </div>
                )}

                {/* 회원 관리 */}
                {!isLoading && tab === 'users' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>회원 목록</h3>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                            <tr style={{ background: '#f8f8f8' }}>
                                {['ID', '아이디', '닉네임', '권한', '가입일', '관리'].map(h => (
                                    <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13, color: '#666', borderBottom: '1px solid #eee' }}>{h}</th>
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
                                        <span style={{ fontSize: 12, padding: '2px 10px', borderRadius: 10, background: user.role === 'ADMIN' ? '#1a1a2e' : '#f0f0f0', color: user.role === 'ADMIN' ? '#fff' : '#333' }}>{user.role}</span>
                                    </td>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{new Date(user.createdAt).toLocaleDateString()}</td>
                                    <td style={{ padding: '12px 16px', display: 'flex', gap: 6 }}>
                                        <button onClick={() => handleRoleChange(user)} style={{ padding: '5px 12px', fontSize: 12, border: '1px solid #ddd', borderRadius: 6, cursor: 'pointer', background: '#fff' }}>{user.role === 'ADMIN' ? 'USER로 변경' : 'ADMIN으로 변경'}</button>
                                        <button onClick={() => handleDeleteUser(user)} style={{ padding: '5px 12px', fontSize: 12, border: 'none', borderRadius: 6, cursor: 'pointer', background: '#ff4d4f', color: '#fff' }}>삭제</button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {pagination(totalPages, page, loadUsers)}
                    </div>
                )}

                {/* 콘텐츠 삭제 */}
                {!isLoading && tab === 'content' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>게시글 목록</h3>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                            <tr style={{ background: '#f8f8f8' }}>
                                {['ID', '제목', '작성일', '관리'].map(h => (
                                    <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13, color: '#666', borderBottom: '1px solid #eee' }}>{h}</th>
                                ))}
                            </tr>
                            </thead>
                            <tbody>
                            {posts.map(post => (
                                <tr key={post.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{post.id}</td>
                                    <td style={{ padding: '12px 16px', fontSize: 14, cursor: 'pointer' }} onClick={() => navigate(`/posts/${post.id}`)}>{post.title}</td>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{new Date(post.createdAt).toLocaleDateString()}</td>
                                    <td style={{ padding: '12px 16px', display: 'flex', gap: 6 }}>
                                        <button onClick={() => navigate(`/posts/${post.id}`)} style={{ padding: '5px 12px', fontSize: 12, border: '1px solid #ddd', borderRadius: 6, cursor: 'pointer', background: '#fff' }}>보기</button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {pagination(postTotalPages, postPage, loadPosts)}
                    </div>
                )}

                {/* 신고 관리 */}
                {!isLoading && tab === 'report' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>신고 목록</h3>
                        <div style={{ marginBottom: 16 }}>
                            <select value={reportStatusFilter} onChange={e => {
                                const val = e.target.value as ReportStatus | '';
                                setReportStatusFilter(val);
                                loadReports(0, val);
                            }} style={inputStyle}>
                                <option value="">상태 전체</option>
                                <option value="PENDING">PENDING (대기중)</option>
                                <option value="RESOLVED">RESOLVED (삭제 처리됨)</option>
                                <option value="REJECTED">REJECTED (반려됨)</option>
                            </select>
                        </div>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                            <tr style={{ background: '#f8f8f8' }}>
                                {['ID', '대상', '사유', '상세', '신고자', '상태', '신고일', '관리'].map(h => (
                                    <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13, color: '#666', borderBottom: '1px solid #eee' }}>{h}</th>
                                ))}
                            </tr>
                            </thead>
                            <tbody>
                            {reports.map(report => {
                                const reasonLabel: Record<string, string> = {
                                    SPAM: '스팸/광고', ABUSE: '욕설/비방', OBSCENE: '음란물', OTHER: '기타',
                                };
                                return (
                                    <tr key={report.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                                        <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{report.id}</td>
                                        <td
                                            style={{ padding: '12px 16px', fontSize: 14, cursor: report.targetType === 'POST' ? 'pointer' : 'default' }}
                                            onClick={() => report.targetType === 'POST' && navigate(`/posts/${report.targetId}`)}
                                        >
                                            {report.targetType === 'POST' ? '게시글' : '댓글'} #{report.targetId}
                                        </td>
                                        <td style={{ padding: '12px 16px', fontSize: 13 }}>{reasonLabel[report.reason] ?? report.reason}</td>
                                        <td style={{ padding: '12px 16px', fontSize: 13, color: '#666', maxWidth: 200 }}>{report.detail || '-'}</td>
                                        <td style={{ padding: '12px 16px', fontSize: 14 }}>{report.reporterNickname}</td>
                                        <td style={{ padding: '12px 16px' }}>
                                            <span style={{
                                                fontSize: 12, padding: '2px 10px', borderRadius: 10,
                                                background: report.status === 'PENDING' ? '#fff3cd' : report.status === 'RESOLVED' ? '#fdecea' : '#f0f0f0',
                                                color: report.status === 'PENDING' ? '#856404' : report.status === 'RESOLVED' ? '#c0392b' : '#666',
                                            }}>{report.status}</span>
                                        </td>
                                        <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{new Date(report.createdAt).toLocaleDateString()}</td>
                                        <td style={{ padding: '12px 16px', display: 'flex', gap: 6 }}>
                                            {report.status === 'PENDING' ? (
                                                <>
                                                    <button onClick={() => handleResolveReport(report, 'DELETE')} style={{ padding: '5px 12px', fontSize: 12, border: 'none', borderRadius: 6, cursor: 'pointer', background: '#ff4d4f', color: '#fff' }}>삭제</button>
                                                    <button onClick={() => handleResolveReport(report, 'REJECT')} style={{ padding: '5px 12px', fontSize: 12, border: '1px solid #ddd', borderRadius: 6, cursor: 'pointer', background: '#fff' }}>반려</button>
                                                </>
                                            ) : (
                                                <span style={{ fontSize: 12, color: '#bbb' }}>처리 완료</span>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                        {pagination(reportTotalPages, reportPage, (i) => loadReports(i, reportStatusFilter))}
                    </div>
                )}

                {/* 구독 회원 관리 (F-41: Enum 값 반영) */}
                {!isLoading && tab === 'subscription' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>구독 회원 목록</h3>
                        <div style={{ marginBottom: 16 }}>
                            <select value={subStatusFilter} onChange={e => {
                                const val = e.target.value as SubscriptionStatus | '';
                                setSubStatusFilter(val);
                                loadSubscriptions(0, val);
                            }} style={inputStyle}>
                                <option value="">상태 전체</option>
                                <option value="ACTIVE">ACTIVE (구독 중)</option>
                                <option value="PAST_DUE">PAST_DUE</option>
                                <option value="EXPIRED">EXPIRED (만료됨)</option>
                            </select>
                        </div>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                            <tr style={{ background: '#f8f8f8' }}>
                                {['ID', '회원', '유형', '상태', '다음 결제일', '결제 시작일'].map(h => (
                                    <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13, color: '#666', borderBottom: '1px solid #eee' }}>{h}</th>
                                ))}
                            </tr>
                            </thead>
                            <tbody>
                            {subscriptions.map(sub => (
                                <tr key={sub.subscriptionId} style={{ borderBottom: '1px solid #f0f0f0' }}>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{sub.subscriptionId}</td>
                                    <td style={{ padding: '12px 16px', fontSize: 14 }}>{sub.nickname} ({sub.username})</td>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#666' }}>
                                        {sub.type === 'RECURRING' ? '정기결제' : '1개월 이용권'}
                                    </td>
                                    <td style={{ padding: '12px 16px' }}>
                                        <span style={{
                                            fontSize: 12, padding: '2px 10px', borderRadius: 10,
                                            background:  sub.status === 'ACTIVE' ? '#e6f4ea' :
                                                         sub.status === 'PAST_DUE' ? '#fff3cd' : '#f0f0f0',
                                            color: sub.status === 'ACTIVE' ? '#1e7e34' :
                                                   sub.status === 'PAST_DUE' ? '#856404' : '#666',
                                        }}>{sub.status}</span>
                                    </td>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{sub.nextBillingAt ? new Date(sub.nextBillingAt).toLocaleDateString() : '-'}</td>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{sub.canceledAt ? new Date(sub.canceledAt).toLocaleDateString() : '-'}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {pagination(subTotalPages, subPage, (i) => loadSubscriptions(i, subStatusFilter))}
                    </div>
                )}

                {/* 결제 내역 (F-42: Enum 값 및 회원 ID 검색 반영) */}
                {!isLoading && tab === 'payment' && (
                    <div>
                        <h3 style={{ marginBottom: 16 }}>결제 내역</h3>
                        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                            <select value={payStatusFilter} onChange={e => {
                                const val = e.target.value as PaymentStatus | '';
                                setPayStatusFilter(val);
                                loadPayments(0, val, payUserIdFilter ? Number(payUserIdFilter) : undefined);
                            }} style={inputStyle}>
                                <option value="">상태 전체</option>
                                <option value="READY">READY (대기)</option>
                                <option value="PAID">PAID (완료)</option>
                                <option value="FAILED">FAILED (실패)</option>
                            </select>

                            <input
                                type="number"
                                placeholder="회원 ID 검색"
                                value={payUserIdFilter}
                                onChange={e => setPayUserIdFilter(e.target.value)}
                                style={inputStyle}
                            />
                            <button onClick={() => loadPayments(0, payStatusFilter, payUserIdFilter ? Number(payUserIdFilter) : undefined)} style={{
                                padding: '10px 16px', border: '1px solid #ddd', borderRadius: 8, background: '#fff', cursor: 'pointer'
                            }}>검색</button>
                        </div>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                            <tr style={{ background: '#f8f8f8' }}>
                                {['ID', '회원', '금액', '회차', '상태', '결제일'].map(h => (
                                    <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13, color: '#666', borderBottom: '1px solid #eee' }}>{h}</th>
                                ))}
                            </tr>
                            </thead>
                            <tbody>
                            {payments.map(payment => (
                                <tr key={payment.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{payment.id}</td>
                                    <td style={{ padding: '12px 16px', fontSize: 14 }}>{payment.username} (ID: {payment.userId})</td>
                                    <td style={{ padding: '12px 16px', fontSize: 14 }}>{payment.amount.toLocaleString()} {payment.currency}</td>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{payment.round}회차</td>
                                    <td style={{ padding: '12px 16px' }}>
                                        <span style={{
                                            fontSize: 12, padding: '2px 10px', borderRadius: 10,
                                            background: payment.status === 'PAID' ? '#e6f4ea' : payment.status === 'FAILED' ? '#fdecea' : '#f0f0f0',
                                            color: payment.status === 'PAID' ? '#1e7e34' : payment.status === 'FAILED' ? '#c0392b' : '#666',
                                        }}>{payment.status}</span>
                                    </td>
                                    <td style={{ padding: '12px 16px', fontSize: 13, color: '#999' }}>{payment.paidAt ? new Date(payment.paidAt).toLocaleDateString() : '-'}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {pagination(payTotalPages, payPage, (i) => loadPayments(i, payStatusFilter, payUserIdFilter ? Number(payUserIdFilter) : undefined))}
                    </div>
                )}
            </div>
        </div>
    );
}