import { useEffect, useState } from 'react';
import { getMyEmojis } from '../api/emojiApi';
import type { Emoji } from '../api/emojiApi';
import { insertEmojiToken } from '../utils/emojiText';

// F-26/27 게시글·댓글 작성/수정 화면에서 쓰는 이모지 선택 패널
// 구독 여부를 프론트에서 별도로 추적하지 않고, 패널을 열 때마다 GET /api/v1/emojis/me를 호출해서
// 403이면 그 응답 메시지를 그대로 보여줌 (백엔드가 매 요청마다 실시간으로 구독 상태를 검증하는 것과 동일한 방식)
//
// 이모지를 클릭하면 텍스트 커서 위치에 [emoji:ID] 토큰을 바로 삽입함 - 글자와 이모지가
// 같은 본문(content) 안에 섞여 저장되고, 조회 화면에서는 EmojiText가 그 토큰을 실제 이미지로 바꿔서 보여줌.
export default function EmojiPicker({
    textareaRef, value, onChange, closeSignal,
}: {
    textareaRef: React.RefObject<HTMLTextAreaElement | null>;
    value: string;
    onChange: (value: string) => void;
    // 게시글/댓글 등록에 성공할 때마다 값을 바꿔서 넘겨주면 열려있던 패널을 닫아줌
    closeSignal?: unknown;
}) {
    const [open, setOpen] = useState(false);
    const [emojis, setEmojis] = useState<Emoji[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        setOpen(false);
    }, [closeSignal]);

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

    const handlePick = (emojiId: number) => {
        const cursor = textareaRef.current?.selectionStart ?? value.length;
        const { text, cursor: nextCursor } = insertEmojiToken(value, cursor, emojiId);
        onChange(text);
        // requestAnimationFrame은 탭이 백그라운드일 때 실행이 미뤄질 수 있어서 setTimeout 사용.
        // React가 controlled textarea의 value를 반영한 뒤에 커서를 다시 맞춰줌
        setTimeout(() => {
            const el = textareaRef.current;
            if (el) {
                el.focus();
                el.setSelectionRange(nextCursor, nextCursor);
            }
        }, 0);
    };

    return (
        <div style={{ position: 'relative', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8, maxWidth: '100%' }}>
            <button type="button" onClick={handleToggleOpen} style={{
                padding: '6px 12px', borderRadius: 8, border: '1px solid #ddd', background: '#fff',
                cursor: 'pointer', fontSize: 13, flexShrink: 0,
            }}>
                😀
            </button>

            {open && (
                <div style={{
                    position: 'absolute', top: '110%', left: 0, zIndex: 50,
                    background: '#fff', border: '1px solid #ddd', borderRadius: 8,
                    boxShadow: '0 4px 12px rgba(0,0,0,0.1)', padding: 10, minWidth: 200, maxWidth: 280,
                }}>
                    {loading ? (
                        <span style={{ fontSize: 13, color: '#999' }}>불러오는 중...</span>
                    ) : error ? (
                        <span style={{ fontSize: 13, color: '#e03131' }}>{error}</span>
                    ) : emojis && emojis.length > 0 ? (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                            {emojis.map(e => (
                                <button key={e.id} type="button"
                                        onMouseDown={ev => ev.preventDefault()}
                                        onClick={() => handlePick(e.id)} title={e.name} style={{
                                    width: 36, height: 36, borderRadius: 8, cursor: 'pointer', padding: 2,
                                    border: '1px solid #eee', background: '#fafafa',
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
