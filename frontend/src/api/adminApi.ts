import axiosInstance from './axiosInstance';

export interface DailyStats {
    date: string;
    newUserCount: number;
    newPostCount: number;
    activeUserCount: number;
}

export interface AdminUser {
    id: number;
    username: string;
    nickname: string;
    role: string;
    createdAt: string;
}

export interface PageResult<T> {
    content: T[];
    totalPages: number;
    number: number;
}

export const getDailyStats = () =>
    axiosInstance.get<{ data: DailyStats }>('/api/v1/admin/statistics/daily').then(r => r.data.data);

export const getUsers = (page = 0) =>
    axiosInstance.get<{ data: PageResult<AdminUser> }>('/api/v1/admin/users', { params: { page, size: 10 } }).then(r => r.data.data);

export const updateRole = (userId: number, role: string) =>
    axiosInstance.put(`/api/v1/admin/users/${userId}/role`, { role });

export const adminDeletePost = (postId: number) =>
    axiosInstance.delete(`/api/v1/admin/posts/${postId}`);

export const adminDeleteComment = (commentId: number) =>
    axiosInstance.delete(`/api/v1/admin/comments/${commentId}`);
