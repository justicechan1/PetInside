import axiosInstance from './axiosInstance';

export interface PageResult<T> {
    content: T[];
    totalPages: number;
    number: number;
}

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

// 백엔드 SubscriptionStatus: ACTIVE | EXPIRED
export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED';

export interface Subscription {
    subscriptionId: number;
    userId: number;
    username: string;
    nickname: string;
    status: SubscriptionStatus;
    type: 'RECURRING' | 'ONE_TIME';
    nextBillingAt: string | null;
    canceledAt: string | null;
}

// 백엔드 PaymentStatus: READY | PAID | FAILED
export type PaymentStatus = 'READY' | 'PAID' | 'FAILED';

export interface Payment {
    id: number;
    userId: number;
    username: string;
    amount: number;
    currency: string;
    round: number;
    status: PaymentStatus;
    paidAt: string | null;
}

// API 함수 정의
export const getDailyStats = () =>
    axiosInstance.get<{ data: DailyStats }>('/api/v1/admin/statistics/daily').then(r => r.data.data);

export const getUsers = (page = 0) =>
    axiosInstance.get<{ data: PageResult<AdminUser> }>('/api/v1/admin/users', { params: { page, size: 10 } }).then(r => r.data.data);

export const updateRole = (userId: number, role: string) =>
    axiosInstance.put(`/api/v1/admin/users/${userId}/role`, { role });

export const deleteUser = (userId: number) =>
    axiosInstance.delete(`/api/v1/admin/users/${userId}`);

export const adminDeletePost = (postId: number) =>
    axiosInstance.delete(`/api/v1/admin/posts/${postId}`);

export const adminDeleteComment = (commentId: number) =>
    axiosInstance.delete(`/api/v1/admin/comments/${commentId}`);

// F-41: 구독 회원 목록 (status, startDate, endDate 지원)
export const getSubscriptions = (page = 0, status?: SubscriptionStatus | '', startDate?: string, endDate?: string) =>
    axiosInstance.get<{ data: PageResult<Subscription> }>('/api/v1/admin/subscriptions', {
        params: { page, size: 10, status: status || undefined, startDate, endDate },
    }).then(r => r.data.data);

// F-42: 결제 목록 (status, userId 지원)
export const getPayments = (page = 0, status?: PaymentStatus | '', userId?: number) =>
    axiosInstance.get<{ data: PageResult<Payment> }>('/api/v1/admin/payments', {
        params: { page, size: 10, status: status || undefined, userId: userId || undefined },
    }).then(r => r.data.data);