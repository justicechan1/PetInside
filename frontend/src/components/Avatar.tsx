export default function Avatar({ imageUrl, nickname, size = 32 }: { imageUrl?: string | null; nickname: string; size?: number }) {
    if (imageUrl) {
        return (
            <img
                src={imageUrl}
                alt=""
                style={{ width: size, height: size, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
            />
        );
    }

    return (
        <div style={{
            width: size, height: size, borderRadius: '50%', flexShrink: 0,
            background: 'var(--primary, #FF8C00)', color: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 'bold', fontSize: size * 0.4,
        }}>
            {nickname ? nickname[0].toUpperCase() : '👤'}
        </div>
    );
}
