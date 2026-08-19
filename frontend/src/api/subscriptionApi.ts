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

export interface OneTimePaymentPrepareResult {
    paymentId: string;
    storeId: string;
    channelKey: string;
    amount: number;
    currency: string;
}

export interface SubscriptionMeResult {
    hasSubscription: boolean;
    subscriptionId: number | null;
    type: 'RECURRING' | 'ONE_TIME' | null;
    status: 'ACTIVE' | 'EXPIRED' | null;
    startAt: string | null;
    nextBillingAt: string | null;
    canceledAt: string | null;
}

export interface PaymentHistoryItem {
    paymentId: string;
    status: 'READY' | 'PAID' | 'FAILED';
    round: number;
    amount: number;
    currency: string;
    paidAt: string | null;
    createdAt: string;
}

export const prepareBillingKey = () =>
    axiosInstance.post<{ data: BillingKeyPrepareResult }>('/api/v1/billing-keys/prepare').then(r => r.data.data);

export const createBillingKey = (issueId: string, billingKey: string) =>
    axiosInstance.post<{ data: BillingKeyCreateResult }>('/api/v1/billing-keys', { issueId, billingKey }).then(r => r.data.data);

export const createSubscription = (billingKeyId: number) =>
    axiosInstance.post<{ data: SubscriptionResult }>('/api/v1/subscriptions', { billingKeyId }).then(r => r.data.data);

export const prepareOneTimePurchase = () =>
    axiosInstance.post<{ data: OneTimePaymentPrepareResult }>('/api/v1/subscriptions/one-time/prepare').then(r => r.data.data);

export const completeOneTimePurchase = (paymentId: string) =>
    axiosInstance.post<{ data: SubscriptionResult }>('/api/v1/subscriptions/one-time/complete', { paymentId }).then(r => r.data.data);

export const getMySubscription = () =>
    axiosInstance.get<{ data: SubscriptionMeResult }>('/api/v1/subscriptions/me').then(r => r.data.data);

export const cancelSubscription = (subscriptionId: number) =>
    axiosInstance.patch<{ data: SubscriptionMeResult }>(`/api/v1/subscriptions/${subscriptionId}/cancel`).then(r => r.data.data);

export const resumeSubscription = (subscriptionId: number) =>
    axiosInstance.patch<{ data: SubscriptionMeResult }>(`/api/v1/subscriptions/${subscriptionId}/resume`).then(r => r.data.data);

export const getPaymentHistory = () =>
    axiosInstance.get<{ data: PaymentHistoryItem[] }>('/api/v1/payments').then(r => r.data.data);
