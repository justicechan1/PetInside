import axiosInstance from './axiosInstance';

export interface UserInfo {
    username: string;
    nickname: string;
    profileImageUrl: string | null;
    role: string;
    createdAt: string;
    provider: string | null;
}

export interface MyPost {
    id: number;
    category: string;
    title: string;
    thumbnailImageUrl: string | null;
    createdAt: string;
}

export const getMyInfo = () =>
    axiosInstance.get<{ data: UserInfo }>('/api/v1/users/me').then(r => r.data.data);

export const updateNickname = (nickname: string) =>
    axiosInstance.patch('/api/v1/users/me/nickname', { nickname });

export const updatePassword = (currentPassword: string, newPassword: string) =>
    axiosInstance.patch('/api/v1/users/me/password', { currentPassword, newPassword });

export const updateProfileImage = (imageUrl: string) =>
    axiosInstance.patch('/api/v1/users/me/profile-image', { imageUrl });

export const getMyPosts = (params?: { keyword?: string; category?: string; page?: number; size?: number }) =>
    axiosInstance.get<{ data: { content: MyPost[]; totalPages: number; number: number } }>('/api/v1/users/me/posts', { params: { size: 10, ...params } }).then(r => r.data.data);
