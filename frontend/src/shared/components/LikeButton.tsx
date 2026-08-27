import { useNavigate } from 'react-router-dom';
import { useLike, type LikeTargetType } from '../hooks/useLike';
import './LikeButton.css';

interface LikeButtonProps {
    targetType: LikeTargetType;
    targetId: number;
}

// 게시글 상세/댓글 목록 어디서든 targetType과 targetId만 넘기면 재사용 가능한 좋아요 버튼.
// 작성자 본인도 클릭 가능, 중복 좋아요는 서버에서 토글로 처리되므로 클릭 = 무조건 토글.
export default function LikeButton({ targetType, targetId }: LikeButtonProps) {
    const navigate = useNavigate();
    const isLoggedIn = !!localStorage.getItem('accessToken');
    const { liked, likeCount, isPending, toggle } = useLike(targetType, targetId);

    const handleClick = () => {
        if (!isLoggedIn) {
            navigate('/login');
            return;
        }
        toggle();
    };

    return (
        <button
            type="button"
            className={`like-button${liked ? ' like-button--active' : ''}`}
            onClick={handleClick}
            disabled={isPending}
            aria-pressed={liked}
        >
            <span className="like-button__icon">{liked ? '♥' : '♡'}</span>
            <span className="like-button__count">{likeCount}</span>
        </button>
    );
}
