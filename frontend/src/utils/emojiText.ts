import type { Emoji } from '../api/emojiApi';

// 본문 텍스트 안에 이모지를 [emoji:ID] 토큰으로 직접 심어서, 글자와 이모지가 한 문장 안에서 섞이게 함.
// (별도 emojiIds 배열은 유지하되, 그 값은 항상 본문에서 추출한 결과 — 텍스트가 유일한 source of truth)
const EMOJI_TOKEN_RE = /\[emoji:(\d+)\]/g;
const EMOJI_TOKEN_SPLIT_RE = /(\[emoji:\d+\])/g;

export function extractEmojiIds(text: string): number[] {
    const ids: number[] = [];
    for (const match of text.matchAll(EMOJI_TOKEN_RE)) {
        const id = Number(match[1]);
        if (!ids.includes(id)) ids.push(id);
    }
    return ids;
}

export function insertEmojiToken(text: string, cursor: number, emojiId: number): { text: string; cursor: number } {
    const token = `[emoji:${emojiId}]`;
    const safeCursor = Math.max(0, Math.min(cursor, text.length));
    return {
        text: text.slice(0, safeCursor) + token + text.slice(safeCursor),
        cursor: safeCursor + token.length,
    };
}

export interface EmojiTextPart {
    key: number;
    type: 'text' | 'emoji';
    text?: string;
    emoji?: Emoji;
}

// 렌더링용 파싱 - 토큰을 실제 Emoji 객체와 매칭해 텍스트/이모지 조각으로 분리
export function parseEmojiText(text: string, emojis: Emoji[]): EmojiTextPart[] {
    return text.split(EMOJI_TOKEN_SPLIT_RE)
        .filter(part => part.length > 0)
        .map((part, i) => {
            const match = part.match(/^\[emoji:(\d+)\]$/);
            if (match) {
                const emoji = emojis.find(e => e.id === Number(match[1]));
                if (emoji) return { key: i, type: 'emoji' as const, emoji };
            }
            return { key: i, type: 'text' as const, text: part };
        });
}

// 구버전(본문에 토큰 없이 emojis만 따로 붙어있던) 게시글/댓글을 수정할 때, 기존에 붙어있던 이모지를 잃어버리지 않도록
// 본문 끝에 토큰을 이어붙여줌. 이미 토큰이 있으면(신규 작성분) 그대로 둠.
export function withLegacyEmojiTokens(content: string, emojis: Emoji[]): string {
    if (!emojis || emojis.length === 0) return content;
    if (extractEmojiIds(content).length > 0) return content;
    return content + emojis.map(e => `[emoji:${e.id}]`).join('');
}
