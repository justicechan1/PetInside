import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getCommentLikeStatus,
  getPostLikeStatus,
  toggleCommentLike,
  togglePostLike,
} from '../api/likeApi';

export type LikeTargetType = 'post' | 'comment';

// 게시글/댓글 좋아요 상태를 관리하는 훅. 클릭 시 즉시 화면에 반영하고(낙관적 업데이트),
// 서버 응답이 오면 실제 값으로 맞추며, 실패하면 이전 상태로 되돌린다.
export function useLike(targetType: LikeTargetType, targetId: number) {
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [isPending, setIsPending] = useState(false);
  const isMounted = useRef(true);

  useEffect(() => {
    isMounted.current = true;
    const fetchStatus = targetType === 'post' ? getPostLikeStatus : getCommentLikeStatus;

    fetchStatus(targetId)
      .then((status) => {
        if (isMounted.current) {
          setLiked(status.liked);
          setLikeCount(status.likeCount);
        }
      })
      .catch(() => {
        // 초기 상태 조회 실패 시 기본값(0, false)을 유지한다.
      });

    return () => {
      isMounted.current = false;
    };
  }, [targetType, targetId]);

  const toggle = useCallback(async () => {
    if (isPending) return;

    const prevLiked = liked;
    const prevCount = likeCount;

    setLiked(!prevLiked);
    setLikeCount(prevLiked ? prevCount - 1 : prevCount + 1);
    setIsPending(true);

    try {
      const toggleFn = targetType === 'post' ? togglePostLike : toggleCommentLike;
      const result = await toggleFn(targetId);
      if (isMounted.current) {
        setLiked(result.liked);
        setLikeCount(result.likeCount);
      }
    } catch (error) {
      if (isMounted.current) {
        setLiked(prevLiked);
        setLikeCount(prevCount);
      }
    } finally {
      if (isMounted.current) {
        setIsPending(false);
      }
    }
  }, [targetType, targetId, liked, likeCount, isPending]);

  return { liked, likeCount, isPending, toggle };
}
