import axiosInstance from './axiosInstance';
import type { Emoji } from './emojiApi';

export interface CommentItem {
    id: number;
    content: string;
    authorId: number;
    authorNickname: string;
    authorProfileImageUrl: string | null;
    authorVerified: boolean;
    emojis: Emoji[];
    createdAt: string;
    children: CommentItem[] | null;
}

export const getComments = (postId: number) =>
    axiosInstance.get<{ data: CommentItem[] }>(`/api/v1/posts/${postId}/comments`).then(r => r.data.data);

export const createComment = (postId: number, body: { content: string; parentId?: number | null; emojiIds?: number[] }) =>
    axiosInstance.post(`/api/v1/posts/${postId}/comments`, body).then(r => r.data);

export const updateComment = (commentId: number, body: { content: string; emojiIds?: number[] }) =>
    axiosInstance.put(`/api/v1/comments/${commentId}`, body).then(r => r.data);

export const deleteComment = (commentId: number) =>
    axiosInstance.delete(`/api/v1/comments/${commentId}`).then(r => r.data);
