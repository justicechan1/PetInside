export default function VerifiedBadge({ size = 14 }: { size?: number }) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 22 22"
            role="img"
            aria-label="인증된 구독자"
            style={{ flexShrink: 0 }}
        >
            <title>인증된 구독자</title>
            <path
                fill="var(--primary, #FF8C00)"
                d="M11 0l2.35 1.9 2.95-.6 1.2 2.8 2.8 1.2-.6 2.95L21.5 11l-1.8 2.35.6 2.95-2.8 1.2-1.2 2.8-2.95-.6L11 22l-2.35-1.9-2.95.6-1.2-2.8-2.8-1.2.6-2.95L.5 11l1.8-2.35-.6-2.95 2.8-1.2 1.2-2.8 2.95.6z"
            />
            <path
                fill="#fff"
                d="M9.6 14.9L5.9 11.2l1.3-1.3 2.4 2.4 5.2-5.2 1.3 1.3z"
            />
        </svg>
    );
}
