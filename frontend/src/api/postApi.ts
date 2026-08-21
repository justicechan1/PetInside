import axiosInstance from './axiosInstance';
import type { Emoji } from './emojiApi';

export interface PostListItem {
    id: number;
    title: string;
    category: string;
    authorId: number;
    authorNickname: string;
    authorProfileImageUrl: string | null;
    authorVerified: boolean;
    viewCount: number;
    commentCount: number;
    thumbnailUrl: string | null;
    createdAt: string;
}

export interface PostDetail {
    id: number;
    title: string;
    content: string;
    category: string;
    viewCount: number;
    authorId: number;
    authorNickname: string;
    authorProfileImageUrl: string | null;
    authorVerified: boolean;
    imageUrls: string[];
    emojis: Emoji[];
    createdAt: string;
    updatedAt: string;
}

export interface PageResult<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    number: number;
}

export const getPosts = (params: { category?: string; keyword?: string; page?: number; size?: number }) =>
    axiosInstance.get<{ data: PageResult<PostListItem> }>('/api/v1/posts', { params }).then(r => r.data.data);

export const getPost = (postId: number) =>
    axiosInstance.get<{ data: PostDetail }>(`/api/v1/posts/${postId}`).then(r => r.data.data);

export const createPost = (body: { category: string; title: string; content: string; imageUrls?: string[]; emojiIds?: number[] }) =>
    axiosInstance.post('/api/v1/posts', body).then(r => r.data);

export const updatePost = (postId: number, body: { category: string; title: string; content: string; imageUrls?: string[]; emojiIds?: number[] }) =>
    axiosInstance.put(`/api/v1/posts/${postId}`, body).then(r => r.data);

export const deletePost = (postId: number) =>
    axiosInstance.delete(`/api/v1/posts/${postId}`).then(r => r.data);
