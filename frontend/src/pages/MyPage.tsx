import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { getMyInfo, updateNickname, updatePassword, updateProfileImage, getMyPosts, updateProfileLayout } from '../api/mypageApi';
import { uploadImage } from '../api/imageApi';
import type { UserInfo, MyPost } from '../api/mypageApi';
import { getMySubscription, getPaymentHistory } from '../api/subscriptionApi';
import type { SubscriptionMeResult, PaymentHistoryItem } from '../api/subscriptionApi';
import { getMyPets, createPet, updatePet } from '../api/petApi';
import type { Pet, PetForm } from '../api/petApi';
import { isAuthenticated } from '../utils/auth';

type Tab = 'profile' | 'posts' | 'pets' | 'payment';

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

    // ??? ?좎? ?뺣낫 ???????????????????????????????????????????????????
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

    // ??? 寃뚯떆湲 ??????????????????????????????????????????????????????
    const [posts, setPosts] = useState<MyPost[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);
    const [categoryFilter, setCategoryFilter] = useState('');
    const [layout, setLayout] = useState<'LIST' | 'GRID'>('LIST');

    // ??? 援щ룆 ????????????????????????????????????????????????????????
    const [subscription, setSubscription] = useState<SubscriptionMeResult | null>(null);

    // ??? 寃곗젣 ?댁뿭 ???????????????????????????????????????????????????
    const [payments, setPayments] = useState<PaymentHistoryItem[]>([]);
    const [payLoading, setPayLoading] = useState(false);

    // ??? 諛섎젮?숇Ъ ?????????????????????????????????????????????????????
    const [pets, setPets] = useState<Pet[]>([]);
    const [petsLoaded, setPetsLoaded] = useState(false);
    const emptyPetForm: PetForm = { petName: '', petType: '', petBirthday: '', petIntro: '', petImageUrl: '' };
    const [showAddForm, setShowAddForm] = useState(false);
    const [editingPet, setEditingPet] = useState<Pet | null>(null);
    const [petForm, setPetForm] = useState<PetForm>(emptyPetForm);
    const [petImageFile, setPetImageFile] = useState<File | null>(null);
    const [petImagePreview, setPetImagePreview] = useState<string | null>(null);
    const [petError, setPetError] = useState('');
    const [petSaving, setPetSaving] = useState(false);
    const petFileInputRef = useRef<HTMLInputElement>(null);

    // ??? 珥덇린 濡쒕뱶 ???????????????????????????????????????????????????
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
        if (tab !== 'pets' || petsLoaded) return;
        getMyPets().then(data => { setPets(data); setPetsLoaded(true); }).catch(() => setPetsLoaded(true));
    }, [tab, petsLoaded]);

    useEffect(() => {
        if (tab !== 'payment') return;
        setPayLoading(true);
        getPaymentHistory().then(setPayments).catch(() => {}).finally(() => setPayLoading(false));
    }, [tab]);

    // ??? ?꾨줈???대?吏 ????????????????????????????????????????????????
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
            setImageError(e.response?.data?.message ?? '?꾨줈???대?吏 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎.');
        }
    };

    const handleProfileImageCancel = () => {
        setSelectedFile(null);
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        setPreviewUrl(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    // ??? ?됰꽕??鍮꾨?踰덊샇 ??????????????????????????????????????????????
    const handleNicknameUpdate = async () => {
        setNicknameError('');
        if (!nickname.trim()) { setNicknameError('?됰꽕?꾩쓣 ?낅젰?댁＜?몄슂.'); return; }
        if (nickname === userInfo?.nickname) { setNicknameError('?꾩옱 ?됰꽕?꾧낵 ?숈씪?⑸땲??'); return; }
        try {
            await updateNickname(nickname);
            alert('?됰꽕?꾩씠 蹂寃쎈릺?덉뒿?덈떎.');
            localStorage.setItem('nickname', nickname);
            window.dispatchEvent(new StorageEvent('storage', { key: 'nickname', newValue: nickname }));
            setUserInfo(prev => prev ? { ...prev, nickname } : prev);
        } catch (e: any) {
            setNicknameError(e.response?.data?.message ?? '?됰꽕??蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎.');
        }
    };

    const handlePasswordUpdate = async () => {
        setPasswordError('');
        if (!currentPassword || !newPassword) { setPasswordError('鍮꾨?踰덊샇瑜??낅젰?댁＜?몄슂.'); return; }
        try {
            await updatePassword(currentPassword, newPassword);
            alert('鍮꾨?踰덊샇媛 蹂寃쎈릺?덉뒿?덈떎.');
            setCurrentPassword('');
            setNewPassword('');
        } catch (e: any) {
            setPasswordError(e.response?.data?.message ?? '鍮꾨?踰덊샇 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎.');
        }
    };

    // ??? ?덉씠?꾩썐 ?좉? ????????????????????????????????????????????????
    const handleLayoutToggle = async (newLayout: 'LIST' | 'GRID') => {
        if (newLayout === layout) return;
        const prev = layout;
        setLayout(newLayout);
        await updateProfileLayout(newLayout).catch(() => setLayout(prev));
    };

    // ??? 諛섎젮?숇Ъ ?????????????????????????????????????????????????????
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
        if (!petForm.petName.trim()) { setPetError('???대쫫???낅젰?댁＜?몄슂.'); return; }
        if (!petForm.petType.trim()) { setPetError('??醫낅쪟瑜??낅젰?댁＜?몄슂.'); return; }
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
            setPetError(e.response?.data?.message ?? '???以??ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.');
        } finally {
            setPetSaving(false);
        }
    };

    const isSubscriber = subscription?.status === 'ACTIVE';

    // ??? ?ㅽ????곸닔 ???????????????????????????????????????????????????
    const tabBtnStyle = (active: boolean): React.CSSProperties => ({
        padding: '10px 20px', border: 'none', borderRadius: 20, cursor: 'pointer', fontWeight: 600, fontSize: 14,
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

    // ??? ???몃씪?????????????????????????????????????????????????????
    const renderPetForm = (title: string) => (
        <div style={{ ...cardStyle, padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ fontWeight: 700, fontSize: 15, color: '#222' }}>{title}</div>

            {/* ?대?吏 ?낅줈??*/}
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
                        ? <img src={petImagePreview} alt="誘몃━蹂닿린" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        : petForm.petImageUrl
                            ? <img src={petForm.petImageUrl} alt="?꾩옱" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                            : '?맽'}
                </div>
                <div>
                    <div style={{ fontSize: 13, color: '#666', marginBottom: 6 }}>?ъ쭊???대┃?댁꽌 蹂寃쏀븯?몄슂</div>
                    <button onClick={() => petFileInputRef.current?.click()} style={{ ...outlineBtn, fontSize: 12, padding: '5px 12px' }}>
                        ?ъ쭊 ?좏깮
                    </button>
                </div>
                <input ref={petFileInputRef} type="file" accept="image/*" onChange={handlePetImageSelect} style={{ display: 'none' }} />
            </div>

            <input placeholder="?대쫫 *" value={petForm.petName}
                onChange={e => setPetForm(p => ({ ...p, petName: e.target.value }))} style={inputStyle} />
            <input placeholder="醫낅쪟 * (?? 怨⑤뱺由ы듃由щ쾭, ?섎Ⅴ?쒖븞 怨좎뼇??" value={petForm.petType}
                onChange={e => setPetForm(p => ({ ...p, petType: e.target.value }))} style={inputStyle} />
            <input type="date" value={petForm.petBirthday}
                onChange={e => setPetForm(p => ({ ...p, petBirthday: e.target.value }))} style={inputStyle} />
            <textarea placeholder="?뚭컻 (?좏깮)" value={petForm.petIntro}
                onChange={e => setPetForm(p => ({ ...p, petIntro: e.target.value }))}
                rows={3} style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }} />

            {petError && <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{petError}</p>}

            <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={handlePetSave} disabled={petSaving} style={{ ...solidBtn, opacity: petSaving ? 0.7 : 1 }}>
                    {petSaving ? '???以?..' : '???}
                </button>
                <button onClick={closePetForm} style={outlineBtn}>痍⑥냼</button>
            </div>
        );

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />
            <div style={{ maxWidth: 680, margin: '0 auto', padding: '32px 16px' }}>

                {/* ??*/}
                <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
                    <button style={tabBtnStyle(tab === 'profile')} onClick={() => setTab('profile')}>?꾨줈??/button>
                    <button style={tabBtnStyle(tab === 'posts')} onClick={() => setTab('posts')}>??寃뚯떆湲</button>
                    <button style={tabBtnStyle(tab === 'pets')} onClick={() => setTab('pets')}>?맽 諛섎젮?숇Ъ</button>
                    <button style={tabBtnStyle(tab === 'payment')} onClick={() => setTab('payment')}>援щ룆쨌寃곗젣</button>
                </div>

                {/* ????? ?꾨줈????????? */}
                {tab === 'profile' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                        {/* ?꾨줈??移대뱶 */}
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
                                            ? <img src={previewUrl} alt="誘몃━蹂닿린" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                            : userInfo?.profileImageUrl
                                                ? <img src={userInfo.profileImageUrl} alt="?꾨줈?? style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                : '?뫀'}
                                    </div>
                                    <button
                                        onClick={() => fileInputRef.current?.click()}
                                        title="?꾨줈???ъ쭊 蹂寃?
                                        style={{
                                            position: 'absolute', bottom: 4, right: 4,
                                            width: 26, height: 26, borderRadius: '50%',
                                            border: '2px solid #fff', background: 'var(--primary, #FF8C00)',
                                            color: '#fff', fontSize: 11, cursor: 'pointer',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}
                                    >?륅툘</button>
                                    <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileSelect} style={{ display: 'none' }} />
                                </div>

                                <div style={{ marginTop: 12, textAlign: 'center' }}>
                                    <div style={{ fontWeight: 700, fontSize: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                                        {userInfo?.nickname}
                                        {isSubscriber && <BadgeIcon />}
                                    </div>
                                    <div style={{ color: '#999', fontSize: 14, marginTop: 4 }}>{userInfo?.username}</div>
                                    <div style={{ fontSize: 12, color: '#bbb', marginTop: 6 }}>
                                        媛?낆씪 {userInfo ? new Date(userInfo.createdAt).toLocaleDateString() : '-'}
                                    </div>
                                </div>

                                {selectedFile && (
                                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, marginTop: 12 }}>
                                        <div style={{ display: 'flex', gap: 8 }}>
                                            <button onClick={handleProfileImageSave} style={{
                                                padding: '8px 20px', border: 'none', borderRadius: 8,
                                                background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 13,
                                            }}>???/button>
                                            <button onClick={handleProfileImageCancel} style={{
                                                padding: '8px 20px', border: '1px solid #ddd', borderRadius: 8,
                                                background: '#fff', cursor: 'pointer', fontSize: 13,
                                            }}>痍⑥냼</button>
                                        </div>
                                        {imageError && <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{imageError}</p>}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* ?됰꽕??蹂寃?*/}
                        <div style={cardStyle}>
                            <div style={{ padding: '20px 24px' }}>
                                <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 14, color: '#222' }}>?됰꽕??蹂寃?/div>
                                <div style={{ display: 'flex', gap: 8 }}>
                                    <input value={nickname}
                                        onChange={e => { setNickname(e.target.value); setNicknameError(''); }}
                                        placeholder="???됰꽕??
                                        style={{ ...inputStyle, flex: 1, borderColor: nicknameError ? '#f44336' : '#e0e0e0' }} />
                                    <button onClick={handleNicknameUpdate} style={{
                                        padding: '11px 20px', border: 'none', borderRadius: 8, whiteSpace: 'nowrap',
                                        background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 14,
                                    }}>蹂寃?/button>
                                </div>
                                {nicknameError && <p style={{ margin: '8px 0 0', fontSize: 13, color: '#f44336' }}>{nicknameError}</p>}
                            </div>
                        </div>

                        {/* 鍮꾨?踰덊샇 蹂寃?*/}
                        {!userInfo?.provider && (
                            <div style={cardStyle}>
                                <div style={{ padding: '20px 24px' }}>
                                    <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 14, color: '#222' }}>鍮꾨?踰덊샇 蹂寃?/div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                                        <input type="password" value={currentPassword}
                                            onChange={e => setCurrentPassword(e.target.value)}
                                            placeholder="?꾩옱 鍮꾨?踰덊샇" style={inputStyle} />
                                        <input type="password" value={newPassword}
                                            onChange={e => { setNewPassword(e.target.value); setPasswordError(''); }}
                                            placeholder="??鍮꾨?踰덊샇 (8???댁긽, ?곷Ц+?レ옄+?뱀닔臾몄옄)"
                                            style={{ ...inputStyle, borderColor: passwordError ? '#f44336' : '#e0e0e0' }} />
                                        {passwordError && <p style={{ margin: 0, fontSize: 13, color: '#f44336' }}>{passwordError}</p>}
                                        <button onClick={handlePasswordUpdate} style={{
                                            padding: '11px', border: 'none', borderRadius: 8,
                                            background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 14,
                                        }}>蹂寃?/button>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* ????? ??寃뚯떆湲 ??????? */}
                {tab === 'posts' && (
                    <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                            <div style={{ display: 'flex', gap: 8 }}>
                                {[['', '?꾩껜'], ['QNA', 'Q&A'], ['BOAST', '?먮옉']].map(([val, label]) => (
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
                                        fontSize: 13,
                                    }}>
                                        {l === 'LIST' ? '?? : '??}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {posts.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', marginTop: 40 }}>?묒꽦??寃뚯떆湲???놁뒿?덈떎.</p>
                        ) : layout === 'GRID' ? (
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                                {posts.map(post => (
                                    <div key={post.id} onClick={() => navigate(`/posts/${post.id}`)} style={{
                                        borderRadius: 12, border: '1px solid #f0f0f0', cursor: 'pointer',
                                        background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,0.04)', overflow: 'hidden',
                                    }}>
                                        <div style={{ height: 120, background: '#f5f5f5', overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                            {post.thumbnailImageUrl
                                                ? <img src={post.thumbnailImageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                : <span style={{ fontSize: 32 }}>?뱷</span>}
                                        </div>
                                        <div style={{ padding: '10px 12px' }}>
                                            <span style={{
                                                fontSize: 11, padding: '2px 6px', borderRadius: 8,
                                                background: post.category === 'QNA' ? '#E3F2FD' : '#FFF3E0',
                                                color: post.category === 'QNA' ? '#1565C0' : '#E65100',
                                            }}>
                                                {post.category === 'QNA' ? 'Q&A' : '?먮옉'}
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
                                                {post.category === 'QNA' ? 'Q&A' : '?먮옉'}
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

                {/* ????? ?맽 諛섎젮?숇Ъ ??????? */}
                {tab === 'pets' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                        {!isSubscriber ? (
                            <div style={{ ...cardStyle, padding: '48px 24px', textAlign: 'center' }}>
                                <div style={{ fontSize: 48, marginBottom: 12 }}>?뵏</div>
                                <div style={{ fontWeight: 700, fontSize: 17, marginBottom: 8 }}>?꾨━誘몄뾼 援щ룆???꾩슜 湲곕뒫</div>
                                <div style={{ fontSize: 14, color: '#888', marginBottom: 24 }}>
                                    諛섎젮?숇Ъ ?꾨줈?꾩쓣 ?깅줉?섍퀬 怨듦컻 ?꾨줈?꾩뿉 ?몄텧?대낫?몄슂.
                                </div>
                                <button onClick={() => navigate('/subscription')} style={{ ...solidBtn, padding: '12px 28px', fontSize: 15 }}>
                                    援щ룆?섍린
                                </button>
                            </div>
                        ) : (
                            <>
                                {/* ??移대뱶 紐⑸줉 */}
                                {pets.map(pet => (
                                    <div key={pet.id}>
                                        {/* ?섏젙 ?쇱씠 ?대┛ ??*/}
                                        {editingPet?.id === pet.id ? (
                                            renderPetForm('???뺣낫 ?섏젙')
                                        ) : (
                                            <div style={{ ...cardStyle, padding: '20px 24px' }}>
                                                <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
                                                    {/* ?ъ쭊 */}
                                                    <div style={{
                                                        width: 80, height: 80, borderRadius: '50%',
                                                        background: '#f0f0f0', overflow: 'hidden', flexShrink: 0,
                                                        display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 36,
                                                        border: '3px solid #fff', boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                                                    }}>
                                                        {pet.petImageUrl
                                                            ? <img src={pet.petImageUrl} alt={pet.petName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                            : '?맽'}
                                                    </div>

                                                    {/* ?뺣낫 */}
                                                    <div style={{ flex: 1, minWidth: 0 }}>
                                                        <div style={{ fontWeight: 700, fontSize: 18, marginBottom: 2 }}>{pet.petName}</div>
                                                        <div style={{ fontSize: 14, color: '#666' }}>
                                                            {pet.petType}
                                                            {pet.petBirthday && <span style={{ color: '#bbb' }}> 쨌 {pet.petBirthday}</span>}
                                                        </div>
                                                        {pet.petIntro && (
                                                            <div style={{ fontSize: 14, color: '#444', marginTop: 8, lineHeight: 1.5 }}>
                                                                {pet.petIntro}
                                                            </div>
                                                        )}
                                                    </div>

                                                    {/* ?섏젙 踰꾪듉 */}
                                                    <button onClick={() => openEditPet(pet)} style={outlineBtn}>?섏젙</button>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                ))}

                                {/* ??諛섎젮?숇Ъ 異붽? ??*/}
                                {showAddForm && renderPetForm('??諛섎젮?숇Ъ ?깅줉')}

                                {/* 異붽? 踰꾪듉 */}
                                {!showAddForm && !editingPet && (
                                    <button
                                        onClick={openAddPet}
                                        style={{
                                            width: '100%', padding: '16px', border: '2px dashed #ddd',
                                            borderRadius: 16, background: '#fff', cursor: 'pointer',
                                            fontSize: 15, color: '#999', fontWeight: 600,
                                            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                                        }}
                                    >
                                        <span style={{ fontSize: 20 }}>+</span> ??諛섎젮?숇Ъ 異붽?
                                    </button>
                                )}
                            </>
                        )}
                    </div>
                )}

                {/* ????? 援щ룆쨌寃곗젣 ??????? */}
                {tab === 'payment' && (
                    <div style={cardStyle}>
                        <div style={{ padding: '20px 24px' }}>
                            <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 16, color: '#222' }}>寃곗젣 ?댁뿭</div>
                            {payLoading ? (
                                <p style={{ color: '#999', textAlign: 'center', padding: '20px 0' }}>遺덈윭?ㅻ뒗 以?..</p>
                            ) : payments.length === 0 ? (
                                <div style={{ textAlign: 'center', padding: '32px 0' }}>
                                    <p style={{ color: '#999', margin: '0 0 16px' }}>寃곗젣 ?댁뿭???놁뒿?덈떎.</p>
                                    <button onClick={() => navigate('/subscription')} style={solidBtn}>援щ룆?섎윭 媛湲?/button>
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
                                                    {item.amount.toLocaleString()}{item.currency === 'KRW' ? '?? : ` ${item.currency}`}
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
                                                {item.status === 'PAID' ? '寃곗젣?꾨즺' : item.status === 'FAILED' ? '?ㅽ뙣' : '以鍮꾩쨷'}
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
