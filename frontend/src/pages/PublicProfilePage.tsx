import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GNB from '../components/GNB';
import { getPublicProfile, getPublicPosts } from '../api/mypageApi';
import type { PublicProfile, MyPost } from '../api/mypageApi';
import { getUserPets, getPetPhotos } from '../api/petApi';
import type { Pet, PetPhoto } from '../api/petApi';
import { cardStyle } from '../styles/common';
import PhotoLightbox from '../components/PhotoLightbox';
import PetStoryBubbles from '../components/PetStoryBubbles';
import PetProfileHeader from '../components/PetProfileHeader';
import PetPhotoGrid from '../components/PetPhotoGrid';

type Section = 'posts' | 'pets';

export default function PublicProfilePage() {
    const { userId } = useParams<{ userId: string }>();
    const navigate = useNavigate();
    const uid = Number(userId);

    const [profile, setProfile] = useState<PublicProfile | null>(null);
    const [notFound, setNotFound] = useState(false);
    const [loading, setLoading] = useState(true);
    const [section, setSection] = useState<Section>('posts');

    const [posts, setPosts] = useState<MyPost[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);

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

            {lightbox && (
                <PhotoLightbox photo={lightbox} onClose={() => setLightbox(null)} />
            )}

            <div style={{ maxWidth: 680, margin: '0 auto', padding: '32px 16px', display: 'flex', flexDirection: 'column', gap: 16 }}>

                {/* 프로필 헤더 */}
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
                            <div style={{ display: 'flex', gap: 32, marginTop: 16, justifyContent: 'center' }}>
                                <div style={{ textAlign: 'center' }}>
                                    <div style={{ fontWeight: 700, fontSize: 18 }}>{profile.postCount}</div>
                                    <div style={{ fontSize: 12, color: '#888' }}>게시글</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div style={{ display: 'flex', borderTop: '1px solid #f0f0f0' }}>
                        <button style={sectionBtn(section === 'posts')} onClick={() => setSection('posts')}>📝 게시글</button>
                        <button style={sectionBtn(section === 'pets')} onClick={() => setSection('pets')}>🐾 반려동물</button>
                    </div>
                </div>

                {/* 게시글 섹션 */}
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

                {/* 반려동물 섹션 */}
                {section === 'pets' && (
                    <div style={cardStyle}>
                        {!petsLoaded ? (
                            <p style={{ textAlign: 'center', color: '#bbb', padding: '32px 0' }}>불러오는 중...</p>
                        ) : pets.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#bbb', padding: '48px 0' }}>등록된 반려동물이 없습니다.</p>
                        ) : (
                            <>
                                <div style={{ padding: '20px 20px 16px', borderBottom: '1px solid #f0f0f0' }}>
                                    <PetStoryBubbles
                                        pets={pets}
                                        selectedPetId={selectedPet?.id}
                                        onSelect={setSelectedPet}
                                    />
                                </div>

                                {selectedPet && (
                                    <div style={{ padding: '16px 20px', borderBottom: '1px solid #f0f0f0' }}>
                                        <PetProfileHeader
                                            pet={selectedPet}
                                            photoCount={(petPhotos[selectedPet.id] ?? []).length}
                                        />
                                    </div>
                                )}

                                {selectedPet && (
                                    <PetPhotoGrid
                                        photos={petPhotos[selectedPet.id] ?? []}
                                        loading={photosLoading}
                                        onPhotoClick={setLightbox}
                                    />
                                )}
                            </>
                        )}
                    </div>
                )}

            </div>
        </div>
    );
}
