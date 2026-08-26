import type { Pet } from '../api/petApi';

interface Props {
    pets: Pet[];
    selectedPetId?: number;
    onSelect: (pet: Pet) => void;
    onAdd?: () => void;
}

export default function PetStoryBubbles({ pets, selectedPetId, onSelect, onAdd }: Props) {
    return (
        <div style={{ display: 'flex', gap: 16, overflowX: 'auto', scrollbarWidth: 'none' }}>
            {pets.map(pet => (
                <div key={pet.id} onClick={() => onSelect(pet)}
                    style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, cursor: 'pointer', flexShrink: 0 }}>
                    <div style={{
                        width: 64, height: 64, borderRadius: '50%', background: '#f0f0f0', overflow: 'hidden',
                        display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28,
                        border: selectedPetId === pet.id ? '3px solid var(--primary, #FF8C00)' : '3px solid #e0e0e0',
                        boxSizing: 'border-box',
                    }}>
                        {pet.petImageUrl
                            ? <img src={pet.petImageUrl} alt={pet.petName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                            : '🐾'}
                    </div>
                    <span style={{
                        fontSize: 12, maxWidth: 64, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                        fontWeight: selectedPetId === pet.id ? 700 : 400,
                        color: selectedPetId === pet.id ? 'var(--primary, #FF8C00)' : '#555',
                    }}>{pet.petName}</span>
                </div>
            ))}

            {onAdd && (
                <div onClick={onAdd}
                    style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, cursor: 'pointer', flexShrink: 0 }}>
                    <div style={{
                        width: 64, height: 64, borderRadius: '50%', background: '#f8f8f8',
                        border: '3px dashed #ddd', boxSizing: 'border-box',
                        display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24, color: '#bbb',
                    }}>+</div>
                    <span style={{ fontSize: 12, color: '#bbb' }}>추가</span>
                </div>
            )}
        </div>
    );
}
