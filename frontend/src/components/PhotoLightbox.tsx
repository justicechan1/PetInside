import type { PetPhoto } from '../api/petApi';

interface Props {
    photo: PetPhoto;
    onClose: () => void;
    onDelete?: (photoId: number) => void;
}

export default function PhotoLightbox({ photo, onClose, onDelete }: Props) {
    return (
        <div onClick={onClose} style={{
            position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.88)', zIndex: 1000,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: 16,
        }}>
            <button onClick={onClose} style={{
                position: 'absolute', top: 16, right: 16,
                background: 'none', border: 'none', color: '#fff', fontSize: 28, cursor: 'pointer', lineHeight: 1,
            }}>×</button>

            <img src={photo.imageUrl} alt={photo.caption ?? ''}
                style={{ maxWidth: '100%', maxHeight: '70vh', objectFit: 'contain', borderRadius: 8, display: 'block' }}
                onClick={e => e.stopPropagation()} />

            <div style={{ marginTop: 16, textAlign: 'center', width: '100%', maxWidth: 480 }}
                onClick={e => e.stopPropagation()}>
                {photo.caption && (
                    <p style={{ color: '#fff', fontSize: 15, margin: '0 0 8px', lineHeight: 1.5 }}>{photo.caption}</p>
                )}
                <p style={{ color: '#aaa', fontSize: 13, margin: '0 0 16px' }}>
                    {new Date(photo.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })}
                </p>
                {onDelete && (
                    <button onClick={() => onDelete(photo.id)} style={{
                        padding: '8px 20px', border: '1px solid #ff6b6b', borderRadius: 8,
                        background: 'transparent', color: '#ff6b6b', cursor: 'pointer', fontSize: 13,
                    }}>사진 삭제</button>
                )}
            </div>
        </div>
    );
}
