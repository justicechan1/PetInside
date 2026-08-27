import type { Emoji } from '../api/emojiApi';
import { parseEmojiText } from '../../../shared/utils/emojiText';

// 본문 텍스트에 심어진 [emoji:ID] 토큰을 실제 이모지 이미지로 바꿔서 글자와 한 줄에 섞어 보여줌
export default function EmojiText({ text, emojis, size = 20 }: { text: string; emojis: Emoji[]; size?: number }) {
    const parts = parseEmojiText(text, emojis ?? []);

    return (
        <>
            {parts.map(part =>
                part.type === 'emoji' && part.emoji ? (
                    <img key={part.key} src={part.emoji.imageUrl} alt={part.emoji.name} title={part.emoji.name}
                         style={{ width: size, height: size, objectFit: 'contain', verticalAlign: 'text-bottom', margin: '0 1px' }} />
                ) : (
                    <span key={part.key}>{part.text}</span>
                )
            )}
        </>
    );
}
