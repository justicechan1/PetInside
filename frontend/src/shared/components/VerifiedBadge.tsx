import verifiedBadgeImage from '../../assets/verified-badge.png';

export default function VerifiedBadge({ size = 20 }: { size?: number }) {
    return (
        <img
            src={verifiedBadgeImage}
            alt="인증된 구독자"
            title="인증된 구독자"
            width={size}
            height={size}
            style={{ flexShrink: 0, objectFit: 'contain', verticalAlign: 'middle' }}
        />
    );
}
