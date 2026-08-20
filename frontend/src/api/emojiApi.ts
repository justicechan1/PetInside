import axiosInstance from './axiosInstance';

export interface Emoji {
    id: number;
    imageUrl: string;
    name: string;
}

export const getMyEmojis = () =>
    axiosInstance.get<{ data: Emoji[] }>('/api/v1/emojis/me').then(r => r.data.data);
