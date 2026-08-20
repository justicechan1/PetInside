import axiosInstance from './axiosInstance';

export interface UserInfo {
    username: string;
    nickname: string;
    profileImageUrl: string | null;
    role: string;
    createdAt: string;
    provider: string | null;
    profileLayout: string;
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

export const updateProfileLayout = (profileLayout: string) =>
    axiosInstance.patch('/api/v1/mypage/profile-layout', { profileLayout });

export const getMyPosts = (params?: { keyword?: string; category?: string; page?: number; size?: number }) =>
    axiosInstance.get<{ data: { content: MyPost[]; totalPages: number; number: number } }>('/api/v1/users/me/posts', { params: { size: 10, ...params } }).then(r => r.data.data);

export interface PublicProfile {
    id: number;
    nickname: string;
    profileImageUrl: string | null;
    postCount: number;
    badges: string[];
    membershipTier: string | null;
}

export const getPublicProfile = (userId: number) =>
    axiosInstance.get<{ data: PublicProfile }>(`/api/v1/users/${userId}`).then(r => r.data.data);

export const getPublicPosts = (userId: number, params?: { keyword?: string; category?: string; page?: number; size?: number }) =>
    axiosInstance.get<{ data: { content: MyPost[]; totalPages: number; number: number } }>(`/api/v1/users/${userId}/posts`, { params: { size: 10, ...params } }).then(r => r.data.data);
