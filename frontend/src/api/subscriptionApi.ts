import axiosInstance from './axiosInstance';

export interface SubscriptionPrepareResult {
    paymentId: string;
    storeId: string;
    channelKey: string;
    amount: number;
    currency: string;
}

export interface SubscriptionCompleteResult {
    subscriptionId: number;
    status: 'ACTIVE' | 'EXPIRED';
    nextBillingAt: string;
}

export const prepareSubscription = () =>
    axiosInstance.post<{ data: SubscriptionPrepareResult }>('/api/v1/subscriptions/prepare').then(r => r.data.data);

export const completeSubscription = (paymentId: string, billingKey: string) =>
    axiosInstance.post<{ data: SubscriptionCompleteResult }>('/api/v1/subscriptions/complete', { paymentId, billingKey }).then(r => r.data.data);
