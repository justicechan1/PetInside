import { useRef } from 'react';
import type { PetPhoto } from '../api/petApi';

interface Props {
    photos: PetPhoto[];
    loading?: boolean;
    onPhotoClick: (photo: PetPhoto) => void;
    onAdd?: (file: File) => void;
}

export default function PetPhotoGrid({ photos, loading, onPhotoClick, onAdd }: Props) {
    const fileInputRef = useRef<HTMLInputElement>(null);

    if (loading) return <p style={{ textAlign: 'center', color: '#bbb', padding: '32px 0' }}>불러오는 중...</p>;

    return (
        <>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 2, padding: 2 }}>
                {onAdd && (
                    <>
                        <div onClick={() => fileInputRef.current?.click()} style={{
                            aspectRatio: '1', background: '#f8f8f8', cursor: 'pointer',
                            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                            gap: 4, border: '2px dashed #e0e0e0',
                        }}>
                            <span style={{ fontSize: 28, color: '#ccc' }}>+</span>
                            <span style={{ fontSize: 11, color: '#bbb' }}>사진 추가</span>
                        </div>
                        <input ref={fileInputRef} type="file" accept="image/*"
                            onChange={e => { const f = e.target.files?.[0]; if (f) onAdd(f); e.target.value = ''; }}
                            style={{ display: 'none' }} />
                    </>
                )}

                {photos.map(photo => (
                    <div key={photo.id} onClick={() => onPhotoClick(photo)}
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

            {!loading && photos.length === 0 && (
                <p style={{ textAlign: 'center', color: '#bbb', padding: '48px 0', fontSize: 14 }}>
                    {onAdd ? '위의 + 버튼으로 사진을 추가해보세요.' : '아직 사진이 없습니다.'}
                </p>
            )}
        </>
    );
}
