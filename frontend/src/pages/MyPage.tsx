import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { getMyInfo, updateNickname, updatePassword, updateProfileImage, getMyPosts, updateProfileLayout } from '../api/mypageApi';
import { uploadImage } from '../api/imageApi';
import type { UserInfo, MyPost } from '../api/mypageApi';
import { getMySubscription, getPaymentHistory, cancelSubscription, resumeSubscription, retrySubscriptionPayment } from '../api/subscriptionApi';
import type { SubscriptionMeResult, PaymentHistoryItem } from '../api/subscriptionApi';
import { getMyPets, createPet, updatePet } from '../api/petApi';
import type { Pet, PetForm } from '../api/petApi';
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
    const [searchParams] = useSearchParams();
    const initialTab = (searchParams.get('tab') as Tab) || 'profile';
    const [tab, setTab] = useState<Tab>(initialTab);

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
    const [retryLoading, setRetryLoading] = useState(false);

    const [pets, setPets] = useState<Pet[]>([]);
    const [petsLoaded, setPetsLoaded] = useState(false);
    const [layout, setLayout] = useState<'LIST' | 'GRID'>('LIST');
    const [showPetForm, setShowPetForm] = useState(false);
    const [editingPet, setEditingPet] = useState<Pet | null>(null);
    const emptyPetForm: PetForm = { petName: '', petType: '', petBirthday: '', petIntro: '', petImageUrl: '' };
    const [petForm, setPetForm] = useState<PetForm>(emptyPetForm);
    const [petError, setPetError] = useState('');
    const [petSaving, setPetSaving] = useState(false);

    useEffect(() => {
        getMyInfo().then(info => {
            setUserInfo(info);
            setNickname(info.nickname);
            setLayout((info.profileLayout as 'LIST' | 'GRID') || 'LIST');
        });
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
        if (tab !== 'profile' || petsLoaded) return;
        getMyPets().then(data => { setPets(data); setPetsLoaded(true); }).catch(() => setPetsLoaded(true));
    }, [tab, petsLoaded]);

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

    const handleRetryPayment = async () => {
        if (!subscription?.subscriptionId) return;
        setRetryLoading(true);
        try {
            const updated = await retrySubscriptionPayment(subscription.subscriptionId);
            setSubscription(updated);
            if (updated.status === 'PAST_DUE') {
                alert('결제가 아직 실패 상태입니다. 카드 정보를 확인한 뒤 다시 시도해주세요.');
            }
        } catch (e: any) {
            alert(e.response?.data?.message ?? '결제 재시도 중 오류가 발생했습니다.');
        } finally {
            setRetryLoading(false);
        }
    };

    // 유예기간 중 해지는 남은 기간을 기다리지 않고 즉시 종료됨(cancelDuringGracePeriod)
    const handleCancelDuringGracePeriod = async () => {
        if (!subscription?.subscriptionId) return;
        if (!confirm('구독을 지금 바로 종료하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) return;
        try {
            const updated = await cancelSubscription(subscription.subscriptionId);
            setSubscription(updated);
        } catch (e: any) {
            alert(e.response?.data?.message ?? '해지 처리 중 오류가 발생했습니다.');
        }
    };

    const handleLayoutToggle = async (newLayout: 'LIST' | 'GRID') => {
        if (newLayout === layout) return;
        const prev = layout;
        setLayout(newLayout);
        await updateProfileLayout(newLayout).catch(() => setLayout(prev));
    };

    const openAddPet = () => { setEditingPet(null); setPetForm(emptyPetForm); setPetError(''); setShowPetForm(true); };
    const openEditPet = (pet: Pet) => {
        setEditingPet(pet);
        setPetForm({ petName: pet.petName, petType: pet.petType, petBirthday: pet.petBirthday ?? '', petIntro: pet.petIntro ?? '', petImageUrl: pet.petImageUrl ?? '' });
        setPetError('');
        setShowPetForm(true);
    };
    const closePetForm = () => { setShowPetForm(false); setEditingPet(null); };

    const handlePetSave = async () => {
        if (!petForm.petName.trim()) { setPetError('펫 이름을 입력해주세요.'); return; }
        if (!petForm.petType.trim()) { setPetError('펫 종류를 입력해주세요.'); return; }
        setPetSaving(true);
        setPetError('');
        try {
            if (editingPet) {
                const updated = await updatePet(editingPet.id, petForm);
                setPets(prev => prev.map(p => p.id === updated.id ? updated : p));
            } else {
                const created = await createPet(petForm);
                setPets(prev => [...prev, created]);
            }
            closePetForm();
        } catch (e: any) {
            setPetError(e.response?.data?.message ?? '저장 중 오류가 발생했습니다.');
        } finally {
            setPetSaving(false);
        }
    };

    const formatDate = (iso: string) => new Date(iso).toLocaleDateString('ko-KR');

    const isSubscriber = subscription?.status === 'ACTIVE';
    const isRecurring = subscription?.type === 'RECURRING';

    // ─── 스타일 상수 ───────────────────────────────────────────────────
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

    // ─── 구독 상태 배너 (프로필/결제 탭 공용) ──────────────────────────
    const renderSubscriptionBanner = () => {
        if (!subscription) return null;
        const { hasSubscription, status, canceledAt, nextBillingAt } = subscription;

        if (!hasSubscription) return (
            <div style={{ background: '#f5f5f5', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <div style={{ fontWeight: 700, color: '#444', marginBottom: 2 }}>프리미엄 멤버십</div>
                    <div style={{ fontSize: 13, color: '#888' }}>인증뱃지, 펫 프로필, 커스텀 피드 혜택</div>
                </div>
                <button onClick={() => navigate('/subscription')} style={solidBtn}>구독하기</button>
            </div>
        );

        if (status === 'ACTIVE' && canceledAt) return (
            <div style={{ background: '#FFF3E0', border: '1px solid #FFB74D', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <div style={{ fontWeight: 700, color: '#E65100' }}>해지 예약됨</div>
                    {nextBillingAt && <div style={{ fontSize: 13, color: '#BF360C', marginTop: 4 }}>{formatDate(nextBillingAt)}까지 이용 가능</div>}
                </div>
                {isRecurring && <button onClick={handleResume} style={solidBtn}>재개하기</button>}
            </div>
        );

        if (status === 'ACTIVE') return (
            <div style={{ background: '#E7F5FF', border: '1px solid #74C0FC', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <div style={{ fontWeight: 700, color: '#1864AB', display: 'flex', alignItems: 'center', gap: 6 }}>
                        <BadgeIcon /> 프리미엄 구독 이용 중
                    </div>
                    {nextBillingAt && isRecurring && <div style={{ fontSize: 13, color: '#1971C2', marginTop: 4 }}>다음 결제일: {formatDate(nextBillingAt)}</div>}
                    {nextBillingAt && !isRecurring && <div style={{ fontSize: 13, color: '#1971C2', marginTop: 4 }}>{formatDate(nextBillingAt)}까지 이용 가능</div>}
                </div>
                {isRecurring && <button onClick={handleCancel} style={outlineBtn}>해지 예약</button>}
            </div>
        );

        // 정기결제 실패 · 유예기간(PAST_DUE): 혜택은 즉시 차단, [다시 결제]로 재시도하거나 [구독 취소]로 바로 종료
        if (status === 'PAST_DUE') return (
            <div style={{ background: '#FFF5F5', border: '1px solid #FFA8A8', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                <div>
                    <div style={{ fontWeight: 700, color: '#C92A2A' }}>결제에 실패했습니다</div>
                    <div style={{ fontSize: 13, color: '#e03131', marginTop: 4 }}>
                        카드 상태를 확인하고 다시 결제해주세요. 계속 실패하면 구독이 자동으로 종료됩니다.
                    </div>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                    <button onClick={handleCancelDuringGracePeriod} style={outlineBtn}>구독 취소</button>
                    <button onClick={handleRetryPayment} disabled={retryLoading} style={{ ...solidBtn, opacity: retryLoading ? 0.6 : 1 }}>
                        {retryLoading ? '재시도 중…' : '다시 결제'}
                    </button>
                </div>
            </div>
        );

        return (
            <div style={{ background: '#FFF5F5', border: '1px solid #FFA8A8', borderRadius: 12, padding: '18px 20px',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <div style={{ fontWeight: 700, color: '#C92A2A' }}>구독이 만료되었습니다</div>
                    <div style={{ fontSize: 13, color: '#e03131', marginTop: 4 }}>재구독하면 모든 혜택이 즉시 복구됩니다.</div>
                </div>
                <button onClick={() => navigate('/subscription')} style={solidBtn}>재구독하기</button>
            </div>
        );
    };

    // ─── 잠금 배너 (F-35/F-36 비구독자 공용) ──────────────────────────
    const renderLockBanner = (label: string) => (
        <div style={{ textAlign: 'center', padding: '28px 0' }}>
            <div style={{ fontSize: 32, marginBottom: 10 }}>🔒</div>
            <div style={{ fontSize: 14, color: '#888', marginBottom: 16 }}>{label}은 프리미엄 구독자 전용 기능입니다.</div>
            <button onClick={() => navigate('/subscription')} style={solidBtn}>구독하기</button>
        </div>
    );

    // ─── F-35 반려동물 프로필 내용 ────────────────────────────────────
    const renderPetSection = () => {
        if (!isSubscriber) return renderLockBanner('반려동물 프로필');

        if (showPetForm) return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div style={{ fontWeight: 600, fontSize: 14, color: '#444', marginBottom: 2 }}>
                    {editingPet ? '펫 정보 수정' : '새 반려동물 등록'}
                </div>
                <input placeholder="이름 *" value={petForm.petName}
                    onChange={e => setPetForm(p => ({ ...p, petName: e.target.value }))} style={inputStyle} />
                <input placeholder="종류 * (예: 골든 리트리버, 페르시안 고양이)" value={petForm.petType}
                    onChange={e => setPetForm(p => ({ ...p, petType: e.target.value }))} style={inputStyle} />
                <input type="date" value={petForm.petBirthday}
                    onChange={e => setPetForm(p => ({ ...p, petBirthday: e.target.value }))} style={inputStyle} />
                <textarea placeholder="소개 (선택)" value={petForm.petIntro}
                    onChange={e => setPetForm(p => ({ ...p, petIntro: e.target.value }))}
                    rows={3} style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }} />
                <input placeholder="이미지 URL (선택)" value={petForm.petImageUrl}
                    onChange={e => setPetForm(p => ({ ...p, petImageUrl: e.target.value }))} style={inputStyle} />
                {petError && <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{petError}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                    <button onClick={handlePetSave} disabled={petSaving} style={{ ...solidBtn, opacity: petSaving ? 0.7 : 1 }}>
                        {petSaving ? '저장 중...' : '저장'}
                    </button>
                    <button onClick={closePetForm} style={outlineBtn}>취소</button>
                </div>
            </div>
        );

        if (pets.length === 0) return (
            <p style={{ textAlign: 'center', color: '#bbb', padding: '16px 0', margin: 0 }}>
                등록된 반려동물이 없습니다.
            </p>
        );

        return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {pets.map(pet => (
                    <div key={pet.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px', background: '#f8f9fa', borderRadius: 12 }}>
                        <div style={{ width: 52, height: 52, borderRadius: '50%', background: '#e9ecef', overflow: 'hidden', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22 }}>
                            {pet.petImageUrl
                                ? <img src={pet.petImageUrl} alt={pet.petName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                : '🐾'}
                        </div>
                        <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ fontWeight: 700, fontSize: 15 }}>{pet.petName}</div>
                            <div style={{ fontSize: 13, color: '#666', marginTop: 2 }}>{pet.petType}</div>
                            {pet.petBirthday && <div style={{ fontSize: 12, color: '#aaa', marginTop: 1 }}>{pet.petBirthday}</div>}
                            {pet.petIntro && <div style={{ fontSize: 13, color: '#555', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{pet.petIntro}</div>}
                        </div>
                        <button onClick={() => openEditPet(pet)} style={outlineBtn}>수정</button>
                    </div>
                ))}
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
                    <button style={tabBtnStyle(tab === 'payment')} onClick={() => setTab('payment')}>구독·결제</button>
                </div>

                {/* ───── 프로필 탭 ───── */}
                {tab === 'profile' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                        {/* 1. 프로필 카드 */}
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
                                    <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileSelect} style={{ display: 'none' }} />
                                </div>

                                <div style={{ marginTop: 12, textAlign: 'center' }}>
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

                        {/* 2. F-35 반려동물 프로필 */}
                        <div style={cardStyle}>
                            <div style={{ padding: '20px 24px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                                    <div style={{ fontWeight: 700, fontSize: 15, color: '#222', display: 'flex', alignItems: 'center', gap: 6 }}>
                                        🐾 반려동물 프로필
                                        <span style={{ fontSize: 11, background: '#E7F5FF', color: '#1864AB', padding: '2px 8px', borderRadius: 10, fontWeight: 500 }}>구독자 전용</span>
                                    </div>
                                    {isSubscriber && !showPetForm && (
                                        <button onClick={openAddPet} style={solidBtn}>+ 추가</button>
                                    )}
                                </div>
                                {renderPetSection()}
                            </div>
                        </div>

                        {/* 4. 닉네임 변경 */}
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

                        {/* 5. 비밀번호 변경 (소셜 로그인 제외) */}
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

                {/* ───── 내 게시글 탭 ───── */}
                {tab === 'posts' && (
                    <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                            <div style={{ display: 'flex', gap: 8 }}>
                                {[['', '전체'], ['QNA', 'Q&A'], ['BOAST', '자랑']].map(([val, label]) => (
                                    <button key={val} onClick={() => { setCategoryFilter(val); setPage(0); }}
                                        style={tabBtnStyle(categoryFilter === val)}>
                                        {label}
                                    </button>
                                ))}
                            </div>
                            <div style={{ display: 'flex', gap: 4, background: '#f0f0f0', borderRadius: 8, padding: 3 }}>
                                {(['LIST', 'GRID'] as const).map(l => (
                                    <button key={l} onClick={() => handleLayoutToggle(l)} style={{
                                        padding: '6px 14px', border: 'none', borderRadius: 6, cursor: 'pointer',
                                        background: layout === l ? '#fff' : 'transparent',
                                        color: layout === l ? '#333' : '#888',
                                        fontWeight: layout === l ? 700 : 400,
                                        boxShadow: layout === l ? '0 1px 3px rgba(0,0,0,0.12)' : 'none',
                                        fontSize: 13, transition: 'all 0.15s',
                                    }}>
                                        {l === 'LIST' ? '☰' : '⊞'}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {posts.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', marginTop: 40 }}>작성한 게시글이 없습니다.</p>
                        ) : layout === 'GRID' ? (
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                                {posts.map(post => (
                                    <div key={post.id} onClick={() => navigate(`/posts/${post.id}`)} style={{
                                        borderRadius: 12, border: '1px solid #f0f0f0', cursor: 'pointer',
                                        background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,0.04)', overflow: 'hidden',
                                    }}>
                                        <div style={{ height: 120, background: post.thumbnailImageUrl ? undefined : '#f5f5f5', overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                            {post.thumbnailImageUrl
                                                ? <img src={post.thumbnailImageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                : <span style={{ fontSize: 32 }}>📝</span>}
                                        </div>
                                        <div style={{ padding: '10px 12px' }}>
                                            <span style={{
                                                fontSize: 11, padding: '2px 6px', borderRadius: 8,
                                                background: post.category === 'QNA' ? '#E3F2FD' : '#FFF3E0',
                                                color: post.category === 'QNA' ? '#1565C0' : '#E65100',
                                            }}>
                                                {post.category === 'QNA' ? 'Q&A' : '자랑'}
                                            </span>
                                            <div style={{ fontWeight: 600, fontSize: 13, marginTop: 6, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{post.title}</div>
                                            <div style={{ fontSize: 11, color: '#bbb', marginTop: 4 }}>{new Date(post.createdAt).toLocaleDateString()}</div>
                                        </div>
                                    </div>
                                ))}
                            </div>
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

                {/* ───── 구독·결제 탭 (F-24 + F-25 통합) ───── */}
                {tab === 'payment' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                        {/* 구독 상태 배너 */}
                        {renderSubscriptionBanner()}

                        {/* 결제 내역 */}
                        <div style={cardStyle}>
                            <div style={{ padding: '20px 24px' }}>
                                <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 16, color: '#222' }}>결제 내역</div>
                                {payLoading ? (
                                    <p style={{ color: '#999', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</p>
                                ) : payments.length === 0 ? (
                                    <div style={{ textAlign: 'center', padding: '32px 0' }}>
                                        <p style={{ color: '#999', margin: '0 0 16px' }}>결제 내역이 없습니다.</p>
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
                                                        {item.amount.toLocaleString()}{item.currency === 'KRW' ? '원' : ` ${item.currency}`}
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
                    </div>
                )}

            </div>
        </div>
    );
}
