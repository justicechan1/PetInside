import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { getMyInfo, updateNickname, updatePassword, updateProfileImage, getMyPosts, updateProfileLayout } from '../api/mypageApi';
import { uploadImage } from '../api/imageApi';
import type { UserInfo, MyPost } from '../api/mypageApi';
import { getMySubscription, getPaymentHistory } from '../api/subscriptionApi';
import type { SubscriptionMeResult, PaymentHistoryItem } from '../api/subscriptionApi';
import { getMyPets, createPet, updatePet, deletePet, getPetPhotos, addPetPhoto, deletePetPhoto, togglePhotoLike } from '../api/petApi';
import type { Pet, PetForm, PetPhoto } from '../api/petApi';
import { isAuthenticated } from '../utils/auth';
import { cardStyle, solidBtn, outlineBtn, inputStyle as sharedInputStyle } from '../styles/common';
import PhotoLightbox from '../components/PhotoLightbox';
import PetStoryBubbles from '../components/PetStoryBubbles';
import PetProfileHeader from '../components/PetProfileHeader';
import PetPhotoGrid from '../components/PetPhotoGrid';
import VerifiedBadge from '../components/VerifiedBadge';

type Tab = 'profile' | 'posts' | 'pets' | 'payment';

export default function MyPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const initialTab = (searchParams.get('tab') as Tab) || 'profile';
    const [tab, setTab] = useState<Tab>(initialTab);

    useEffect(() => {
        if (!isAuthenticated()) navigate('/login');
    }, [navigate]);

    // ─── 유저 정보 ───────────────────────────────────────────────────
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

    // ─── 게시글 ──────────────────────────────────────────────────────
    const [posts, setPosts] = useState<MyPost[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);
    const [categoryFilter, setCategoryFilter] = useState('');
    const [layout, setLayout] = useState<'LIST' | 'GRID'>('LIST');

    // ─── 구독 ────────────────────────────────────────────────────────
    const [subscription, setSubscription] = useState<SubscriptionMeResult | null>(null);

    // ─── 결제 내역 ───────────────────────────────────────────────────
    const [payments, setPayments] = useState<PaymentHistoryItem[]>([]);
    const [payLoading, setPayLoading] = useState(false);
    const [payPage, setPayPage] = useState(0);
    const [payTotalPages, setPayTotalPages] = useState(0);

    // ─── 반려동물 ─────────────────────────────────────────────────────
    const [pets, setPets] = useState<Pet[]>([]);
    const [petsLoaded, setPetsLoaded] = useState(false);
    const [selectedPet, setSelectedPet] = useState<Pet | null>(null);
    const [petPhotos, setPetPhotos] = useState<{ [petId: number]: PetPhoto[] }>({});
    const [photosLoading, setPhotosLoading] = useState(false);
    const [photoUploading, setPhotoUploading] = useState(false);
    // 업로드 미리보기 모달
    const [uploadPreview, setUploadPreview] = useState<{ file: File; previewUrl: string } | null>(null);
    const [uploadCaption, setUploadCaption] = useState('');
    // 라이트박스
    const [lightbox, setLightbox] = useState<PetPhoto | null>(null);
    const emptyPetForm: PetForm = { petName: '', petType: '', petBirthday: '', petIntro: '', petImageUrl: '' };
    const [showAddForm, setShowAddForm] = useState(false);
    const [editingPet, setEditingPet] = useState<Pet | null>(null);
    const [petForm, setPetForm] = useState<PetForm>(emptyPetForm);
    const [petImageFile, setPetImageFile] = useState<File | null>(null);
    const [petImagePreview, setPetImagePreview] = useState<string | null>(null);
    const [petError, setPetError] = useState('');
    const [petSaving, setPetSaving] = useState(false);
    const petFileInputRef = useRef<HTMLInputElement>(null);

    // ─── 초기 로드 ───────────────────────────────────────────────────
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
        if ((!['pets', 'posts'].includes(tab)) || petsLoaded || subscription?.status !== 'ACTIVE') return;
        getMyPets().then(data => {
            setPets(data);
            setPetsLoaded(true);
            if (data.length > 0) setSelectedPet(data[0]);
        }).catch(() => setPetsLoaded(true));
    }, [tab, petsLoaded, subscription]);

    useEffect(() => {
        if (!selectedPet) return;
        if (petPhotos[selectedPet.id] !== undefined) return;
        setPhotosLoading(true);
        getPetPhotos(selectedPet.id)
            .then(photos => setPetPhotos(prev => ({ ...prev, [selectedPet.id]: photos })))
            .catch(() => setPetPhotos(prev => ({ ...prev, [selectedPet.id]: [] })))
            .finally(() => setPhotosLoading(false));
    }, [selectedPet]);

    useEffect(() => {
        if (tab !== 'payment') return;
        setPayLoading(true);
        getPaymentHistory(payPage)
            .then(result => { setPayments(result.content); setPayTotalPages(result.totalPages); })
            .catch(() => {})
            .finally(() => setPayLoading(false));
    }, [tab, payPage]);

    // ─── 프로필 이미지 ────────────────────────────────────────────────
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

    // ─── 닉네임/비밀번호 ──────────────────────────────────────────────
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

    // ─── 레이아웃 토글 ────────────────────────────────────────────────
    const handleLayoutToggle = async (newLayout: 'LIST' | 'GRID') => {
        if (newLayout === layout) return;
        const prev = layout;
        setLayout(newLayout);
        await updateProfileLayout(newLayout).catch(() => setLayout(prev));
    };

    // ─── 반려동물 ─────────────────────────────────────────────────────
    const openAddPet = () => {
        setEditingPet(null);
        setPetForm(emptyPetForm);
        clearPetImage();
        setPetError('');
        setShowAddForm(true);
    };

    const openEditPet = (pet: Pet) => {
        setEditingPet(pet);
        setPetForm({ petName: pet.petName, petType: pet.petType, petBirthday: pet.petBirthday ?? '', petIntro: pet.petIntro ?? '', petImageUrl: pet.petImageUrl ?? '' });
        clearPetImage();
        setPetError('');
        setShowAddForm(false);
    };

    const closePetForm = () => {
        setEditingPet(null);
        setShowAddForm(false);
        clearPetImage();
    };

    const clearPetImage = () => {
        if (petImagePreview) URL.revokeObjectURL(petImagePreview);
        setPetImageFile(null);
        setPetImagePreview(null);
        if (petFileInputRef.current) petFileInputRef.current.value = '';
    };

    const handlePetImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setPetImageFile(file);
        if (petImagePreview) URL.revokeObjectURL(petImagePreview);
        setPetImagePreview(URL.createObjectURL(file));
    };

    const handlePetSave = async () => {
        if (!petForm.petName.trim()) { setPetError('펫 이름을 입력해주세요.'); return; }
        if (!petForm.petType.trim()) { setPetError('펫 종류를 입력해주세요.'); return; }
        setPetSaving(true);
        setPetError('');
        try {
            let imageUrl = petForm.petImageUrl;
            if (petImageFile) {
                imageUrl = await uploadImage(petImageFile);
            }
            const formToSave = { ...petForm, petImageUrl: imageUrl };

            if (editingPet) {
                const updated = await updatePet(editingPet.id, formToSave);
                setPets(prev => prev.map(p => p.id === updated.id ? updated : p));
            } else {
                const created = await createPet(formToSave);
                setPets(prev => [...prev, created]);
            }
            closePetForm();
        } catch (e: any) {
            setPetError(e.response?.data?.message ?? '저장 중 오류가 발생했습니다.');
        } finally {
            setPetSaving(false);
        }
    };

    const handlePhotoFileSelect = (file: File) => {
        setUploadPreview({ file, previewUrl: URL.createObjectURL(file) });
        setUploadCaption('');
    };

    const handlePhotoUploadConfirm = async () => {
        if (!uploadPreview || !selectedPet) return;
        setPhotoUploading(true);
        try {
            const url = await uploadImage(uploadPreview.file);
            const photo = await addPetPhoto(selectedPet.id, url, uploadCaption.trim() || undefined);
            setPetPhotos(prev => ({ ...prev, [selectedPet.id]: [photo, ...(prev[selectedPet.id] ?? [])] }));
            URL.revokeObjectURL(uploadPreview.previewUrl);
            setUploadPreview(null);
            setUploadCaption('');
        } catch (e: any) {
            alert(e.response?.data?.message ?? '사진 업로드에 실패했습니다.');
        } finally {
            setPhotoUploading(false);
        }
    };

    const handlePhotoDelete = async (photoId: number) => {
        if (!selectedPet) return;
        if (!confirm('이 사진을 삭제하시겠습니까?')) return;
        setLightbox(null);
        try {
            await deletePetPhoto(selectedPet.id, photoId);
            setPetPhotos(prev => ({ ...prev, [selectedPet.id]: (prev[selectedPet.id] ?? []).filter(p => p.id !== photoId) }));
        } catch (e: any) {
            alert(e.response?.data?.message ?? '삭제에 실패했습니다.');
        }
    };

    const handlePetDelete = async (petId: number) => {
        if (!confirm('반려동물 프로필과 모든 사진이 삭제됩니다. 계속하시겠습니까?')) return;
        try {
            await deletePet(petId);
            const next = pets.filter(p => p.id !== petId);
            setPets(next);
            setPetPhotos(prev => { const n = { ...prev }; delete n[petId]; return n; });
            setSelectedPet(next.length > 0 ? next[0] : null);
        } catch (e: any) {
            alert(e.response?.data?.message ?? '삭제에 실패했습니다.');
        }
    };

    const selectPet = (pet: Pet) => {
        setSelectedPet(pet);
        setEditingPet(null);
        setShowAddForm(false);
    };

    const isSubscriber = subscription?.status === 'ACTIVE';

    // ─── 스타일 상수 ───────────────────────────────────────────────────
    const inputStyle = sharedInputStyle;

    const tabBtnStyle = (active: boolean): React.CSSProperties => ({
        padding: '10px 20px', border: 'none', borderRadius: 20, cursor: 'pointer', fontWeight: 600, fontSize: 14,
        background: active ? 'var(--primary, #FF8C00)' : '#f0f0f0',
        color: active ? '#fff' : '#333',
    });

    // ─── 펫 인라인 폼 ─────────────────────────────────────────────────
    const renderPetForm = (title: string) => (
        <div style={{ ...cardStyle, padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ fontWeight: 700, fontSize: 15, color: '#222' }}>{title}</div>

            {/* 이미지 업로드 */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div
                    onClick={() => petFileInputRef.current?.click()}
                    style={{
                        width: 72, height: 72, borderRadius: '50%', background: '#f0f0f0',
                        border: '2px dashed #ccc', overflow: 'hidden', flexShrink: 0,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        cursor: 'pointer', fontSize: 28,
                    }}
                >
                    {petImagePreview
                        ? <img src={petImagePreview} alt="미리보기" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        : petForm.petImageUrl
                            ? <img src={petForm.petImageUrl} alt="현재" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                            : '🐾'}
                </div>
                <div>
                    <div style={{ fontSize: 13, color: '#666', marginBottom: 6 }}>사진을 클릭해서 변경하세요</div>
                    <button onClick={() => petFileInputRef.current?.click()} style={{ ...outlineBtn, fontSize: 12, padding: '5px 12px' }}>
                        사진 선택
                    </button>
                </div>
                <input ref={petFileInputRef} type="file" accept="image/*" onChange={handlePetImageSelect} style={{ display: 'none' }} />
            </div>

            <input placeholder="이름 *" value={petForm.petName}
                onChange={e => setPetForm(p => ({ ...p, petName: e.target.value }))} style={inputStyle} />
            <input placeholder="종류 * (예: 골든리트리버, 페르시안 고양이)" value={petForm.petType}
                onChange={e => setPetForm(p => ({ ...p, petType: e.target.value }))} style={inputStyle} />
            <input type="date" value={petForm.petBirthday}
                onChange={e => setPetForm(p => ({ ...p, petBirthday: e.target.value }))} style={inputStyle} />
            <textarea placeholder="소개 (선택)" value={petForm.petIntro}
                onChange={e => setPetForm(p => ({ ...p, petIntro: e.target.value }))}
                rows={3} style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }} />

            {petError && <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{petError}</p>}

            <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={handlePetSave} disabled={petSaving} style={{ ...solidBtn, opacity: petSaving ? 0.7 : 1 }}>
                    {petSaving ? '저장 중...' : '저장'}
                </button>
                <button onClick={closePetForm} style={outlineBtn}>취소</button>
            </div>
        </div>
    );

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />

            {/* ── 사진 업로드 미리보기 모달 ── */}
            {uploadPreview && (
                <div onClick={() => { URL.revokeObjectURL(uploadPreview.previewUrl); setUploadPreview(null); }}
                    style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.75)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
                    <div onClick={e => e.stopPropagation()}
                        style={{ background: '#fff', borderRadius: 16, width: '100%', maxWidth: 400, overflow: 'hidden' }}>
                        <img src={uploadPreview.previewUrl} alt="미리보기" style={{ width: '100%', aspectRatio: '1', objectFit: 'cover', display: 'block' }} />
                        <div style={{ padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
                            <input
                                value={uploadCaption}
                                onChange={e => setUploadCaption(e.target.value)}
                                placeholder="코멘트 입력 (선택)"
                                maxLength={200}
                                style={{ padding: '10px 14px', border: '1px solid #e0e0e0', borderRadius: 8, fontSize: 14, width: '100%', boxSizing: 'border-box' }}
                            />
                            <div style={{ display: 'flex', gap: 8 }}>
                                <button onClick={handlePhotoUploadConfirm} disabled={photoUploading}
                                    style={{ ...solidBtn, flex: 1, opacity: photoUploading ? 0.7 : 1 }}>
                                    {photoUploading ? '업로드 중...' : '업로드'}
                                </button>
                                <button onClick={() => { URL.revokeObjectURL(uploadPreview.previewUrl); setUploadPreview(null); }}
                                    style={{ ...outlineBtn, flex: 1 }}>취소</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* ── 라이트박스 모달 ── */}
            {lightbox && (
                <PhotoLightbox
                    photo={lightbox}
                    onClose={() => setLightbox(null)}
                    onDelete={handlePhotoDelete}
                />
            )}
            <div style={{ maxWidth: 680, margin: '0 auto', padding: '32px 16px' }}>

                {/* 탭 */}
                <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
                    <button style={tabBtnStyle(tab === 'profile')} onClick={() => setTab('profile')}>프로필</button>
                    <button style={tabBtnStyle(tab === 'posts')} onClick={() => setTab('posts')}>내 게시글</button>
                    <button style={tabBtnStyle(tab === 'pets')} onClick={() => setTab('pets')}>🐾 반려동물</button>
                    <button style={tabBtnStyle(tab === 'payment')} onClick={() => setTab('payment')}>구독·결제</button>
                </div>

                {/* ───── 프로필 탭 ───── */}
                {tab === 'profile' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

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
                                    <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileSelect} style={{ display: 'none' }} />
                                </div>

                                <div style={{ marginTop: 12, textAlign: 'center' }}>
                                    <div style={{ fontWeight: 700, fontSize: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                                        {userInfo?.nickname}
                                        {isSubscriber && <VerifiedBadge size={28} />}
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

                        {/* 닉네임 변경 */}
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

                        {/* 비밀번호 변경 */}
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
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                        {/* ── 구독자 전용: 펫 스토리 버블 ── */}
                        {isSubscriber && petsLoaded && pets.length > 0 && (
                            <div style={{ ...cardStyle, padding: '16px 20px' }}>
                                <div style={{ fontSize: 12, color: '#aaa', marginBottom: 10, fontWeight: 600, letterSpacing: 0.5 }}>MY PETS</div>
                                <PetStoryBubbles
                                    pets={pets}
                                    onSelect={pet => { setSelectedPet(pet); setTab('pets'); }}
                                />
                            </div>
                        )}

                        {/* ── 구독자 전용: 펫은 있지만 아직 미로드 ── */}
                        {isSubscriber && !petsLoaded && (
                            <div style={{ ...cardStyle, padding: '16px 20px', color: '#bbb', fontSize: 13 }}>펫 정보 불러오는 중...</div>
                        )}

                        {/* ── 피드 컨트롤 헤더 ── */}
                        <div style={{ ...cardStyle, padding: '14px 20px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div style={{ display: 'flex', gap: 6 }}>
                                    {[['', '전체'], ['QNA', 'Q&A'], ['BOAST', '자랑']].map(([val, label]) => (
                                        <button key={val} onClick={() => { setCategoryFilter(val); setPage(0); }}
                                            style={tabBtnStyle(categoryFilter === val)}>
                                            {label}
                                        </button>
                                    ))}
                                </div>
                                {/* 레이아웃 토글: 구독자만 */}
                                {isSubscriber ? (
                                    <div style={{ display: 'flex', gap: 2, background: '#f0f0f0', borderRadius: 8, padding: 3 }}>
                                        {(['LIST', 'GRID'] as const).map(l => (
                                            <button key={l} onClick={() => handleLayoutToggle(l)} style={{
                                                padding: '6px 14px', border: 'none', borderRadius: 6, cursor: 'pointer',
                                                background: layout === l ? '#fff' : 'transparent',
                                                color: layout === l ? '#333' : '#888',
                                                fontWeight: layout === l ? 700 : 400,
                                                boxShadow: layout === l ? '0 1px 3px rgba(0,0,0,0.12)' : 'none',
                                                fontSize: 13,
                                            }}>
                                                {l === 'LIST' ? '☰' : '⊞'}
                                            </button>
                                        ))}
                                    </div>
                                ) : (
                                    <span style={{ fontSize: 12, color: '#bbb', cursor: 'default' }} title="구독자 전용">☰</span>
                                )}
                            </div>
                        </div>

                        {/* ── 게시글 목록 ── */}
                        {posts.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', marginTop: 24 }}>작성한 게시글이 없습니다.</p>
                        ) : (isSubscriber && layout === 'GRID') ? (
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                                {posts.map(post => (
                                    <div key={post.id} onClick={() => navigate(`/posts/${post.id}`)} style={{
                                        borderRadius: 12, border: '1px solid #f0f0f0', cursor: 'pointer',
                                        background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,0.04)', overflow: 'hidden',
                                    }}>
                                        <div style={{ height: 120, background: '#f5f5f5', overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
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
                            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 8 }}>
                                {Array.from({ length: totalPages }, (_, i) => (
                                    <button key={i} onClick={() => setPage(i)} style={{
                                        width: 36, height: 36, borderRadius: 8, border: 'none', cursor: 'pointer',
                                        background: page === i ? 'var(--primary, #FF8C00)' : '#f0f0f0',
                                        color: page === i ? '#fff' : '#333', fontWeight: 600,
                                    }}>{i + 1}</button>
                                ))}
                            </div>
                        )}

                        {/* ── 비구독자 구독 유도 ── */}
                        {!isSubscriber && (
                            <div style={{
                                borderRadius: 12, border: '1px dashed #ffd8a8',
                                background: '#fff9f0', padding: '16px 20px',
                                display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12,
                            }}>
                                <div>
                                    <div style={{ fontWeight: 700, fontSize: 14, color: '#e67700', marginBottom: 4 }}>🌟 프리미엄 구독 혜택</div>
                                    <div style={{ fontSize: 13, color: '#888' }}>그리드 뷰 · 펫 프로필 피드를 이용해보세요</div>
                                </div>
                                <button onClick={() => navigate('/subscription')} style={{ ...solidBtn, flexShrink: 0 }}>구독하기</button>
                            </div>
                        )}
                    </div>
                )}

                {/* ───── 🐾 반려동물 탭 ───── */}
                {tab === 'pets' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>

                        {!isSubscriber ? (
                            <div style={{ ...cardStyle, padding: '48px 24px', textAlign: 'center' }}>
                                <div style={{ fontSize: 48, marginBottom: 12 }}>🔒</div>
                                <div style={{ fontWeight: 700, fontSize: 17, marginBottom: 8 }}>프리미엄 구독자 전용 기능</div>
                                <div style={{ fontSize: 14, color: '#888', marginBottom: 24 }}>
                                    반려동물 프로필을 등록하고 피드를 꾸며보세요.
                                </div>
                                <button onClick={() => navigate('/subscription')} style={{ ...solidBtn, padding: '12px 28px', fontSize: 15 }}>
                                    구독하기
                                </button>
                            </div>
                        ) : (
                            <div style={cardStyle}>
                                {/* -- Story bubbles */}
                                <div style={{ padding: '20px 20px 16px', borderBottom: '1px solid #f0f0f0' }}>
                                    <PetStoryBubbles pets={pets} selectedPetId={selectedPet?.id} onSelect={selectPet} onAdd={openAddPet} />
                                </div>

                                {/* ── 선택된 펫 프로필 헤더 ── */}
                                {selectedPet && !showAddForm && (
                                    <div style={{ padding: '16px 20px', borderBottom: '1px solid #f0f0f0' }}>
                                        {editingPet?.id === selectedPet.id ? (
                                            renderPetForm('펫 정보 수정')
                                        ) : (
                                            <PetProfileHeader
                                                pet={selectedPet}
                                                photoCount={(petPhotos[selectedPet.id] ?? []).length}
                                                onEdit={() => openEditPet(selectedPet)}
                                                onDelete={() => handlePetDelete(selectedPet.id)}
                                            />
                                        )}
                                    </div>
                                )}

                                {/* ── 새 펫 추가 폼 ── */}
                                {showAddForm && (
                                    <div style={{ padding: '16px 20px', borderBottom: '1px solid #f0f0f0' }}>
                                        {renderPetForm('새 반려동물 등록')}
                                    </div>
                                )}

                                {/* ── 사진 그리드 피드 ── */}
                                {selectedPet && !showAddForm && !editingPet && (
                                    <PetPhotoGrid
                                        photos={petPhotos[selectedPet.id] ?? []}
                                        loading={photosLoading}
                                        onPhotoClick={setLightbox}
                                        onAdd={handlePhotoFileSelect}
                                        onLike={photo => togglePhotoLike(selectedPet.id, photo.id).then(result =>
                                            setPetPhotos(prev => ({ ...prev, [selectedPet.id]: (prev[selectedPet.id] ?? []).map(p => p.id === photo.id ? { ...p, ...result } : p) }))
                                        )}
                                    />
                                )}
                            </div>
                        )}
                    </div>
                )}

                {/* ───── 구독·결제 탭 ───── */}
                {tab === 'payment' && (
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
                            {payTotalPages > 1 && (
                                <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12, marginTop: 16 }}>
                                    <button onClick={() => setPayPage(p => p - 1)} disabled={payPage === 0} style={{
                                        padding: '8px 14px', border: '1px solid #e0e0e0', borderRadius: 8,
                                        background: '#fff', color: payPage === 0 ? '#ccc' : '#666', fontWeight: 700, fontSize: 14,
                                        cursor: payPage === 0 ? 'default' : 'pointer',
                                    }}>
                                        이전
                                    </button>
                                    <span style={{ fontSize: 14, color: '#666' }}>{payPage + 1} / {payTotalPages}</span>
                                    <button onClick={() => setPayPage(p => p + 1)} disabled={payPage >= payTotalPages - 1} style={{
                                        padding: '8px 14px', border: '1px solid #e0e0e0', borderRadius: 8,
                                        background: '#fff', color: payPage >= payTotalPages - 1 ? '#ccc' : '#666', fontWeight: 700, fontSize: 14,
                                        cursor: payPage >= payTotalPages - 1 ? 'default' : 'pointer',
                                    }}>
                                        다음
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                )}

            </div>
        </div>
    );
}
