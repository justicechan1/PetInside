import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import GNB from '../components/GNB';
import { getMyInfo, updateNickname, updatePassword, updateProfileImage, getMyPosts } from '../api/mypageApi';
import { uploadImage } from '../api/imageApi';
import type { UserInfo, MyPost } from '../api/mypageApi';
import { getMySubscription, getPaymentHistory, cancelSubscription, resumeSubscription } from '../api/subscriptionApi';
import type { SubscriptionMeResult, PaymentHistoryItem } from '../api/subscriptionApi';
import { isAuthenticated } from '../utils/auth';

type Tab = 'profile' | 'posts' | 'payment';

const BadgeIcon = () => (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
        <circle cx="9" cy="9" r="9" fill="#339AF0"/>
        <path d="M5 9.5L7.5 12L13 6.5" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
);

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

    const [subscription, setSubscription] = useState<SubscriptionMeResult | null>(null);
    const [payments, setPayments] = useState<PaymentHistoryItem[]>([]);
    const [payLoading, setPayLoading] = useState(false);

    useEffect(() => {
        getMyInfo().then(info => { setUserInfo(info); setNickname(info.nickname); });
        getMySubscription().then(setSubscription).catch(() => {});
    }, []);

    useEffect(() => {
        if (tab !== 'posts') return;
        getMyPosts({ category: categoryFilter || undefined, page, size: 10 }).then(data => {
            setPosts(data.content);
            setTotalPages(data.totalPages);
        });
    }, [tab, categoryFilter, page]);

    useEffect(() => {
        if (tab !== 'payment') return;
        setPayLoading(true);
        getPaymentHistory().then(setPayments).catch(() => {}).finally(() => setPayLoading(false));
    }, [tab]);

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
            localStorage.setItem('profileImageUrl', url);
            window.dispatchEvent(new StorageEvent('storage', { key: 'profileImageUrl', newValue: url }));
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
        if (!currentPassword || !newPassword) { setPasswordError('비밀번호를 입력해주세요.'); return; }
        try {
            await updatePassword(currentPassword, newPassword);
            alert('비밀번호가 변경되었습니다.');
            setCurrentPassword('');
            setNewPassword('');
        } catch (e: any) {
            setPasswordError(e.response?.data?.message ?? '비밀번호 변경에 실패했습니다.');
        }
    };

    const handleCancel = async () => {
        if (!subscription?.subscriptionId) return;
        if (!confirm('구독을 해지 예약하시겠습니까? 다음 결제일까지는 계속 이용할 수 있습니다.')) return;
        try {
            const updated = await cancelSubscription(subscription.subscriptionId);
            setSubscription(updated);
        } catch (e: any) {
            alert(e.response?.data?.message ?? '해지 처리 중 오류가 발생했습니다.');
        }
    };

    const handleResume = async () => {
        if (!subscription?.subscriptionId) return;
        try {
            const updated = await resumeSubscription(subscription.subscriptionId);
            setSubscription(updated);
        } catch (e: any) {
            alert(e.response?.data?.message ?? '재개 처리 중 오류가 발생했습니다.');
        }
    };

    const formatDate = (iso: string) => new Date(iso).toLocaleDateString('ko-KR');

    const isSubscriber = subscription?.status === 'ACTIVE';
    const isRecurring = subscription?.type === 'RECURRING';

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

    const solidBtn: React.CSSProperties = {
        padding: '8px 18px', border: 'none', borderRadius: 8, whiteSpace: 'nowrap',
        background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 13,
    };

    const outlineBtn: React.CSSProperties = {
        padding: '8px 18px', border: '1px solid #dee2e6', borderRadius: 8, whiteSpace: 'nowrap',
        background: '#fff', cursor: 'pointer', fontSize: 13,
    };

    const renderSubscriptionBanner = () => {
        if (!subscription) return null;

        const { hasSubscription, status, canceledAt, nextBillingAt } = subscription;

        if (!hasSubscription) return (
            <div style={{ background: '#f5f5f5', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, color: '#555' }}>프리미엄 멤버십 혜택을 확인해보세요</span>
                <button onClick={() => navigate('/subscription')} style={solidBtn}>구독하기</button>
            </div>
        );

        // ACTIVE + 해지 예약됨
        if (status === 'ACTIVE' && canceledAt) return (
            <div style={{ background: '#FFF3E0', border: '1px solid #FFB74D', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <div style={{ fontWeight: 700, color: '#E65100' }}>해지 예약됨</div>
                    {nextBillingAt && (
                        <div style={{ fontSize: 13, color: '#BF360C', marginTop: 4 }}>
                            {formatDate(nextBillingAt)}까지 이용 가능
                        </div>
                    )}
                </div>
                {isRecurring && <button onClick={handleResume} style={solidBtn}>재개하기</button>}
            </div>
        );

        // ACTIVE 정상
        if (status === 'ACTIVE') return (
            <div style={{ background: '#E7F5FF', border: '1px solid #74C0FC', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <div style={{ fontWeight: 700, color: '#1864AB', display: 'flex', alignItems: 'center', gap: 6 }}>
                        <BadgeIcon /> 프리미엄 구독 이용 중
                    </div>
                    {nextBillingAt && isRecurring && (
                        <div style={{ fontSize: 13, color: '#1971C2', marginTop: 4 }}>
                            다음 결제일: {formatDate(nextBillingAt)}
                        </div>
                    )}
                    {nextBillingAt && !isRecurring && (
                        <div style={{ fontSize: 13, color: '#1971C2', marginTop: 4 }}>
                            {formatDate(nextBillingAt)}까지 이용 가능
                        </div>
                    )}
                </div>
                {isRecurring && <button onClick={handleCancel} style={outlineBtn}>해지 예약</button>}
            </div>
        );

        // EXPIRED
        return (
            <div style={{ background: '#FFF5F5', border: '1px solid #FFA8A8', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, color: '#C92A2A' }}>구독이 만료되었습니다</span>
                <button onClick={() => navigate('/subscription')} style={solidBtn}>재구독하기</button>
            </div>
        );
    };

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 680, margin: '0 auto', padding: '32px 16px' }}>

                {/* 탭 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                    <button style={tabBtnStyle(tab === 'profile')} onClick={() => setTab('profile')}>프로필</button>
                    <button style={tabBtnStyle(tab === 'posts')} onClick={() => setTab('posts')}>내 게시글</button>
                    <button style={tabBtnStyle(tab === 'payment')} onClick={() => setTab('payment')}>결제내역</button>
                </div>

                {/* 프로필 탭 */}
                {tab === 'profile' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                        {/* F-25 구독 상태 배너 */}
                        {renderSubscriptionBanner()}

                        {/* 프로필 카드 */}
                        <div style={cardStyle}>
                            <div style={{ height: 80, background: 'linear-gradient(135deg, #FF8C00 0%, #ffb347 100%)' }} />

                            <div style={{ padding: '0 28px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: -44 }}>
                                <div style={{ position: 'relative' }}>
                                    <div style={{
                                        width: 88, height: 88, borderRadius: '50%',
                                        border: '4px solid #fff', background: '#eee',
                                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        fontSize: 40, overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
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
                                            border: '2px solid #fff', background: 'var(--primary, #FF8C00)',
                                            color: '#fff', fontSize: 11, cursor: 'pointer',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}
                                    >✏️</button>
                                    <input ref={fileInputRef} type="file" accept="image/*"
                                        onChange={handleFileSelect} style={{ display: 'none' }} />
                                </div>

                                <div style={{ marginTop: 12, textAlign: 'center' }}>
                                    {/* F-30 인증뱃지 */}
                                    <div style={{ fontWeight: 700, fontSize: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                                        {userInfo?.nickname}
                                        {isSubscriber && <BadgeIcon />}
                                    </div>
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
                                        {imageError && <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{imageError}</p>}
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
                                {nicknameError && <p style={{ margin: '8px 0 0', fontSize: 13, color: '#f44336' }}>{nicknameError}</p>}
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
                                            placeholder="새 비밀번호 (8자 이상, 영문+숫자+특수문자)"
                                            style={{ ...inputStyle, borderColor: passwordError ? '#f44336' : '#e0e0e0' }} />
                                        {passwordError && <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{passwordError}</p>}
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

                {/* F-24 결제내역 탭 */}
                {tab === 'payment' && (
                    <div style={cardStyle}>
                        <div style={{ padding: '20px 24px' }}>
                            <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 16, color: '#222' }}>결제 내역</div>
                            {payLoading ? (
                                <p style={{ color: '#999', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</p>
                            ) : payments.length === 0 ? (
                                <div style={{ textAlign: 'center', padding: '32px 0' }}>
                                    <p style={{ color: '#999', marginBottom: 16 }}>결제 내역이 없습니다.</p>
                                    <button onClick={() => navigate('/subscription')} style={solidBtn}>구독하러 가기</button>
                                </div>
                            ) : (
                                <div style={{ display: 'flex', flexDirection: 'column' }}>
                                    {payments.map((item, i) => (
                                        <div key={item.paymentId} style={{
                                            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                            padding: '16px 0', borderTop: i === 0 ? 'none' : '1px solid #f0f0f0',
                                        }}>
                                            <div>
                                                <div style={{ fontWeight: 700, fontSize: 16 }}>
                                                    {item.amount.toLocaleString()}
                                                    {item.currency === 'KRW' ? '원' : ` ${item.currency}`}
                                                </div>
                                                <div style={{ fontSize: 13, color: '#999', marginTop: 4 }}>
                                                    {new Date(item.paidAt ?? item.createdAt).toLocaleString('ko-KR')}
                                                </div>
                                            </div>
                                            <span style={{
                                                fontSize: 13, fontWeight: 700, padding: '4px 12px', borderRadius: 12,
                                                color: item.status === 'PAID' ? '#2e7d32' : item.status === 'FAILED' ? '#c62828' : '#666',
                                                background: item.status === 'PAID' ? '#e8f5e9' : item.status === 'FAILED' ? '#ffebee' : '#f0f0f0',
                                            }}>
                                                {item.status === 'PAID' ? '결제완료' : item.status === 'FAILED' ? '실패' : '준비중'}
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
