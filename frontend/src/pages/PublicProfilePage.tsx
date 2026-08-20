import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { getPublicProfile, getPublicPosts } from '../api/mypageApi';
import type { PublicProfile, MyPost } from '../api/mypageApi';
import { getUserPets, getPetPhotos } from '../api/petApi';
import type { Pet, PetPhoto } from '../api/petApi';

type Section = 'posts' | 'pets';

export default function PublicProfilePage() {
    const { userId } = useParams<{ userId: string }>();
    const navigate = useNavigate();
    const uid = Number(userId);

    const [profile, setProfile] = useState<PublicProfile | null>(null);
    const [notFound, setNotFound] = useState(false);
    const [loading, setLoading] = useState(true);
    const [section, setSection] = useState<Section>('posts');

    // 게시글
    const [posts, setPosts] = useState<MyPost[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);

    // 펫
    const [pets, setPets] = useState<Pet[]>([]);
    const [petsLoaded, setPetsLoaded] = useState(false);
    const [selectedPet, setSelectedPet] = useState<Pet | null>(null);
    const [petPhotos, setPetPhotos] = useState<{ [petId: number]: PetPhoto[] }>({});
    const [photosLoading, setPhotosLoading] = useState(false);
    const [lightbox, setLightbox] = useState<PetPhoto | null>(null);

    useEffect(() => {
        setLoading(true);
        getPublicProfile(uid)
            .then(setProfile)
            .catch(() => setNotFound(true))
            .finally(() => setLoading(false));
    }, [uid]);

    useEffect(() => {
        getPublicPosts(uid, { page, size: 12 }).then(data => {
            setPosts(data.content);
            setTotalPages(data.totalPages);
        });
    }, [uid, page]);

    useEffect(() => {
        if (section !== 'pets' || petsLoaded) return;
        getUserPets(uid)
            .then(data => {
                setPets(data);
                setPetsLoaded(true);
                if (data.length > 0) setSelectedPet(data[0]);
            })
            .catch(() => setPetsLoaded(true));
    }, [section, petsLoaded, uid]);

    useEffect(() => {
        if (!selectedPet || petPhotos[selectedPet.id] !== undefined) return;
        setPhotosLoading(true);
        getPetPhotos(selectedPet.id)
            .then(photos => setPetPhotos(prev => ({ ...prev, [selectedPet.id]: photos })))
            .catch(() => setPetPhotos(prev => ({ ...prev, [selectedPet.id]: [] })))
            .finally(() => setPhotosLoading(false));
    }, [selectedPet]);

    // ─── 스타일 ────────────────────────────────────────────────────────
    const cardStyle: React.CSSProperties = {
        background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)', overflow: 'hidden',
    };

    const sectionBtn = (active: boolean): React.CSSProperties => ({
        flex: 1, padding: '12px 0', border: 'none', background: 'none', cursor: 'pointer',
        fontWeight: active ? 700 : 400, fontSize: 14,
        color: active ? 'var(--primary, #FF8C00)' : '#888',
        borderBottom: active ? '2px solid var(--primary, #FF8C00)' : '2px solid transparent',
    });

    if (loading) return <div><GNB /><p style={{ textAlign: 'center', marginTop: 80 }}>불러오는 중...</p></div>;
    if (notFound || !profile) return <div><GNB /><p style={{ textAlign: 'center', marginTop: 80, color: '#999' }}>존재하지 않는 사용자입니다.</p></div>;

    return (
        <div style={{ background: '#f8f8f8', minHeight: '100vh' }}>
            <GNB />

            {/* 라이트박스 */}
            {lightbox && (
                <div onClick={() => setLightbox(null)}
                    style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.88)', zIndex: 1000, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
                    <button onClick={() => setLightbox(null)}
                        style={{ position: 'absolute', top: 16, right: 16, background: 'none', border: 'none', color: '#fff', fontSize: 28, cursor: 'pointer' }}>×</button>
                    <img src={lightbox.imageUrl} alt={lightbox.caption ?? ''}
                        style={{ maxWidth: '100%', maxHeight: '70vh', objectFit: 'contain', borderRadius: 8 }}
                        onClick={e => e.stopPropagation()} />
                    <div style={{ marginTop: 16, textAlign: 'center' }} onClick={e => e.stopPropagation()}>
                        {lightbox.caption && <p style={{ color: '#fff', fontSize: 15, margin: '0 0 8px' }}>{lightbox.caption}</p>}
                        <p style={{ color: '#aaa', fontSize: 13, margin: 0 }}>
                            {new Date(lightbox.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })}
                        </p>
                    </div>
                </div>
            )}

            <div style={{ maxWidth: 680, margin: '0 auto', padding: '32px 16px', display: 'flex', flexDirection: 'column', gap: 16 }}>

                {/* ── 프로필 헤더 ── */}
                <div style={cardStyle}>
                    <div style={{ height: 80, background: 'linear-gradient(135deg, #FF8C00 0%, #ffb347 100%)' }} />
                    <div style={{ padding: '0 28px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: -44 }}>
                        <div style={{
                            width: 88, height: 88, borderRadius: '50%', border: '4px solid #fff',
                            background: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: 40, overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                        }}>
                            {profile.profileImageUrl
                                ? <img src={profile.profileImageUrl} alt="프로필" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                : '👤'}
                        </div>

                        <div style={{ marginTop: 12, textAlign: 'center' }}>
                            <div style={{ fontWeight: 700, fontSize: 20 }}>{profile.nickname}</div>
                            {profile.membershipTier && (
                                <span style={{
                                    display: 'inline-block', marginTop: 6, fontSize: 12, padding: '2px 10px',
                                    borderRadius: 10, background: '#FFF3E0', color: '#E65100', fontWeight: 600,
                                }}>{profile.membershipTier}</span>
                            )}
                            {profile.badges.length > 0 && (
                                <div style={{ marginTop: 8, display: 'flex', gap: 6, justifyContent: 'center', flexWrap: 'wrap' }}>
                                    {profile.badges.map((b, i) => (
                                        <span key={i} style={{ fontSize: 12, padding: '2px 8px', borderRadius: 10, background: '#E3F2FD', color: '#1565C0' }}>{b}</span>
                                    ))}
                                </div>
                            )}
                            {/* 통계 */}
                            <div style={{ display: 'flex', gap: 32, marginTop: 16, justifyContent: 'center' }}>
                                <div style={{ textAlign: 'center' }}>
                                    <div style={{ fontWeight: 700, fontSize: 18 }}>{profile.postCount}</div>
                                    <div style={{ fontSize: 12, color: '#888' }}>게시글</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 섹션 탭 */}
                    <div style={{ display: 'flex', borderTop: '1px solid #f0f0f0' }}>
                        <button style={sectionBtn(section === 'posts')} onClick={() => setSection('posts')}>📝 게시글</button>
                        <button style={sectionBtn(section === 'pets')} onClick={() => setSection('pets')}>🐾 반려동물</button>
                    </div>
                </div>

                {/* ── 게시글 섹션 ── */}
                {section === 'posts' && (
                    <>
                        {posts.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', marginTop: 24 }}>작성한 게시글이 없습니다.</p>
                        ) : (
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
                        )}
                        {totalPages > 1 && (
                            <div style={{ display: 'flex', justifyContent: 'center', gap: 8 }}>
                                {Array.from({ length: totalPages }, (_, i) => (
                                    <button key={i} onClick={() => setPage(i)} style={{
                                        width: 36, height: 36, borderRadius: 8, border: 'none', cursor: 'pointer',
                                        background: page === i ? 'var(--primary, #FF8C00)' : '#f0f0f0',
                                        color: page === i ? '#fff' : '#333', fontWeight: 600,
                                    }}>{i + 1}</button>
                                ))}
                            </div>
                        )}
                    </>
                )}

                {/* ── 반려동물 섹션 ── */}
                {section === 'pets' && (
                    <div style={cardStyle}>
                        {!petsLoaded ? (
                            <p style={{ textAlign: 'center', color: '#bbb', padding: '32px 0' }}>불러오는 중...</p>
                        ) : pets.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#bbb', padding: '48px 0' }}>등록된 반려동물이 없습니다.</p>
                        ) : (
                            <>
                                {/* 펫 버블 */}
                                <div style={{ display: 'flex', gap: 16, padding: '20px 20px 16px', overflowX: 'auto', borderBottom: '1px solid #f0f0f0', scrollbarWidth: 'none' }}>
                                    {pets.map(pet => (
                                        <div key={pet.id} onClick={() => setSelectedPet(pet)}
                                            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, cursor: 'pointer', flexShrink: 0 }}>
                                            <div style={{
                                                width: 64, height: 64, borderRadius: '50%', background: '#f0f0f0', overflow: 'hidden',
                                                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28,
                                                border: selectedPet?.id === pet.id ? '3px solid var(--primary, #FF8C00)' : '3px solid #e0e0e0',
                                                boxSizing: 'border-box',
                                            }}>
                                                {pet.petImageUrl
                                                    ? <img src={pet.petImageUrl} alt={pet.petName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                    : '🐾'}
                                            </div>
                                            <span style={{
                                                fontSize: 12, maxWidth: 64, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                                                fontWeight: selectedPet?.id === pet.id ? 700 : 400,
                                                color: selectedPet?.id === pet.id ? 'var(--primary, #FF8C00)' : '#555',
                                            }}>{pet.petName}</span>
                                        </div>
                                    ))}
                                </div>

                                {/* 선택된 펫 프로필 헤더 */}
                                {selectedPet && (
                                    <div style={{ padding: '16px 20px', borderBottom: '1px solid #f0f0f0' }}>
                                        <div style={{ display: 'flex', gap: 20, alignItems: 'center' }}>
                                            <div style={{
                                                width: 72, height: 72, borderRadius: '50%', flexShrink: 0,
                                                background: '#f0f0f0', overflow: 'hidden',
                                                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 32,
                                            }}>
                                                {selectedPet.petImageUrl
                                                    ? <img src={selectedPet.petImageUrl} alt={selectedPet.petName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                    : '🐾'}
                                            </div>
                                            <div style={{ flex: 1, minWidth: 0 }}>
                                                <div style={{ fontWeight: 700, fontSize: 17, marginBottom: 4 }}>{selectedPet.petName}</div>
                                                <div style={{ fontSize: 13, color: '#888' }}>
                                                    {selectedPet.petType}
                                                    {selectedPet.petBirthday && <span> · {selectedPet.petBirthday}</span>}
                                                </div>
                                                {selectedPet.petIntro && (
                                                    <div style={{ fontSize: 13, color: '#444', marginTop: 6, lineHeight: 1.5 }}>{selectedPet.petIntro}</div>
                                                )}
                                            </div>
                                            <div style={{ textAlign: 'center', flexShrink: 0 }}>
                                                <div style={{ fontWeight: 700, fontSize: 18 }}>{(petPhotos[selectedPet.id] ?? []).length}</div>
                                                <div style={{ fontSize: 12, color: '#888' }}>게시물</div>
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {/* 사진 그리드 */}
                                {selectedPet && (
                                    photosLoading ? (
                                        <p style={{ textAlign: 'center', color: '#bbb', padding: '32px 0' }}>불러오는 중...</p>
                                    ) : (petPhotos[selectedPet.id] ?? []).length === 0 ? (
                                        <p style={{ textAlign: 'center', color: '#bbb', padding: '48px 0', fontSize: 14 }}>아직 사진이 없습니다.</p>
                                    ) : (
                                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 2, padding: 2 }}>
                                            {(petPhotos[selectedPet.id] ?? []).map(photo => (
                                                <div key={photo.id} onClick={() => setLightbox(photo)}
                                                    style={{ position: 'relative', aspectRatio: '1', overflow: 'hidden', cursor: 'pointer' }}>
                                                    <img src={photo.imageUrl} alt={photo.caption ?? ''}
                                                        style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
                                                    {photo.caption && (
                                                        <div style={{
                                                            position: 'absolute', bottom: 0, left: 0, right: 0,
                                                            background: 'linear-gradient(transparent, rgba(0,0,0,0.55))',
                                                            padding: '16px 6px 5px', fontSize: 10, color: '#fff',
                                                            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                                                        }}>{photo.caption}</div>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )
                                )}
                            </>
                        )}
                    </div>
                )}

            </div>
        </div>
    );
}
