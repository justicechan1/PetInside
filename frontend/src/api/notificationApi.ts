import axiosInstance from './axiosInstance';
import type {PageResult} from './adminApi';

export interface NotificationItem {
    id: number;
    type: string;
    content: string;
    targetId: number;
    isRead: boolean;
    linkUrl: string;
    createdAt: string;
}

// F-34: 알림 목록 조회
export const getNotifications = (page = 0) =>
    axiosInstance.get<{ data: PageResult<NotificationItem> }>('/api/v1/notifications', {
        params: { page, size: 10 },
    }).then(r => r.data.data);

// F-34: 알림 단건 읽음 처리
export const readNotification = (notificationId: number) =>
    axiosInstance.patch(`/api/v1/notifications/${notificationId}/read`).then(r => r.data.data);

// F-34: 구독 요청 함수 추가
export const subscribePush = (subscription: PushSubscriptionJSON) => {
    return axiosInstance.post('/api/v1/notifications/subscribe', subscription);
};