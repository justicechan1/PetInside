import { useState } from 'react';
import { getMyEmojis } from '../api/emojiApi';
import type { Emoji } from '../api/emojiApi';

// F-26/27 게시글·댓글 작성/수정 화면에서 쓰는 이모지 선택 패널
// 구독 여부를 프론트에서 별도로 추적하지 않고, 패널을 열 때마다 GET /api/v1/emojis/me를 호출해서
// 403이면 그 응답 메시지를 그대로 보여줌 (백엔드가 매 요청마다 실시간으로 구독 상태를 검증하는 것과 동일한 방식)
export default function EmojiPicker({ selectedIds, onChange }: { selectedIds: number[]; onChange: (ids: number[]) => void }) {
    const [open, setOpen] = useState(false);
    const [emojis, setEmojis] = useState<Emoji[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const handleToggleOpen = async () => {
        const next = !open;
        setOpen(next);
        if (next && emojis === null && !loading) {
            setLoading(true);
            setError(null);
            try {
                setEmojis(await getMyEmojis());
            } catch (e: any) {
                setError(e.response?.data?.message ?? '이모지를 불러오지 못했습니다.');
            } finally {
                setLoading(false);
            }
        }
    };

    const toggleEmoji = (id: number) => {
        onChange(selectedIds.includes(id) ? selectedIds.filter(i => i !== id) : [...selectedIds, id]);
    };

    const selectedEmojis = (emojis ?? []).filter(e => selectedIds.includes(e.id));

    return (
        <div style={{ position: 'relative', display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <button type="button" onClick={handleToggleOpen} style={{
                padding: '6px 12px', borderRadius: 8, border: '1px solid #ddd', background: '#fff',
                cursor: 'pointer', fontSize: 13,
            }}>
                😀 이모지{selectedIds.length > 0 ? ` (${selectedIds.length})` : ''}
            </button>

            {selectedEmojis.length > 0 && (
                <span style={{ display: 'inline-flex', gap: 4 }}>
                    {selectedEmojis.map(e => (
                        <img key={e.id} src={e.imageUrl} alt={e.name} title={e.name}
                             style={{ width: 20, height: 20, objectFit: 'contain' }} />
                    ))}
                </span>
            )}

            {open && (
                <div style={{
                    position: 'absolute', top: '110%', left: 0, zIndex: 50,
                    background: '#fff', border: '1px solid #ddd', borderRadius: 8,
                    boxShadow: '0 4px 12px rgba(0,0,0,0.1)', padding: 10, minWidth: 200,
                }}>
                    {loading ? (
                        <span style={{ fontSize: 13, color: '#999' }}>불러오는 중...</span>
                    ) : error ? (
                        <span style={{ fontSize: 13, color: '#e03131' }}>{error}</span>
                    ) : emojis && emojis.length > 0 ? (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                            {emojis.map(e => (
                                <button key={e.id} type="button" onClick={() => toggleEmoji(e.id)} title={e.name} style={{
                                    width: 36, height: 36, borderRadius: 8, cursor: 'pointer', padding: 2,
                                    border: selectedIds.includes(e.id) ? '2px solid var(--primary, #FF8C00)' : '1px solid #eee',
                                    background: '#fafafa',
                                }}>
                                    <img src={e.imageUrl} alt={e.name} style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                                </button>
                            ))}
                        </div>
                    ) : (
                        <span style={{ fontSize: 13, color: '#999' }}>사용 가능한 이모지가 없습니다.</span>
                    )}
                </div>
            )}
        </div>
    );
}
