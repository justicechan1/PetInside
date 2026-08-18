import axiosInstance from './axiosInstance';

export interface BillingKeyPrepareResult {
    issueId: string;
    storeId: string;
    channelKey: string;
}

export interface BillingKeyCreateResult {
    billingKeyId: number;
}

export interface SubscriptionResult {
    subscriptionId: number;
    status: 'ACTIVE' | 'EXPIRED';
    nextBillingAt: string;
}

export const prepareBillingKey = () =>
    axiosInstance.post<{ data: BillingKeyPrepareResult }>('/api/v1/billing-keys/prepare').then(r => r.data.data);

export const createBillingKey = (issueId: string, billingKey: string) =>
    axiosInstance.post<{ data: BillingKeyCreateResult }>('/api/v1/billing-keys', { issueId, billingKey }).then(r => r.data.data);

export const createSubscription = (billingKeyId: number) =>
    axiosInstance.post<{ data: SubscriptionResult }>('/api/v1/subscriptions', { billingKeyId }).then(r => r.data.data);
