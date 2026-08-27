import type { Pet } from '../api/petApi';
import { outlineBtn } from '../../../shared/styles/common';

interface Props {
    pet: Pet;
    photoCount: number;
    onEdit?: () => void;
    onDelete?: () => void;
}

export default function PetProfileHeader({ pet, photoCount, onEdit, onDelete }: Props) {
    return (
        <div style={{ display: 'flex', gap: 20, alignItems: 'center' }}>
            <div style={{
                width: 72, height: 72, borderRadius: '50%', flexShrink: 0,
                background: '#f0f0f0', overflow: 'hidden',
                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 32,
            }}>
                {pet.petImageUrl
                    ? <img src={pet.petImageUrl} alt={pet.petName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    : '🐾'}
            </div>

            <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4, flexWrap: 'wrap' }}>
                    <span style={{ fontWeight: 700, fontSize: 17 }}>{pet.petName}</span>
                    {onEdit && (
                        <button onClick={onEdit} style={{ ...outlineBtn, padding: '4px 12px', fontSize: 12 }}>수정</button>
                    )}
                    {onDelete && (
                        <button onClick={onDelete} style={{ ...outlineBtn, padding: '4px 12px', fontSize: 12, color: '#e03131', borderColor: '#ffa8a8' }}>삭제</button>
                    )}
                </div>
                <div style={{ fontSize: 13, color: '#888' }}>
                    {pet.petType}
                    {pet.petBirthday && <span> · {pet.petBirthday}</span>}
                </div>
                {pet.petIntro && (
                    <div style={{ fontSize: 13, color: '#444', marginTop: 6, lineHeight: 1.5 }}>{pet.petIntro}</div>
                )}
            </div>

            <div style={{ textAlign: 'center', flexShrink: 0 }}>
                <div style={{ fontWeight: 700, fontSize: 18 }}>{photoCount}</div>
                <div style={{ fontSize: 12, color: '#888' }}>게시물</div>
            </div>
        </div>
    );
}
