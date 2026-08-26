import axiosInstance from './axiosInstance';

export interface LikeStatus {
    liked: boolean;
    likeCount: number;
}

export const togglePostLike = (postId: number) =>
    axiosInstance.post<{ data: LikeStatus }>(`/api/v1/posts/${postId}/likes`).then(r => r.data.data);

export const getPostLikeStatus = (postId: number) =>
    axiosInstance.get<{ data: LikeStatus }>(`/api/v1/posts/${postId}/likes/me`).then(r => r.data.data);

export const toggleCommentLike = (commentId: number) =>
    axiosInstance.post<{ data: LikeStatus }>(`/api/v1/comments/${commentId}/likes`).then(r => r.data.data);

export const getCommentLikeStatus = (commentId: number) =>
    axiosInstance.get<{ data: LikeStatus }>(`/api/v1/comments/${commentId}/likes/me`).then(r => r.data.data);
