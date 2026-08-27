import axiosInstance from './axiosInstance';

export type ReportTargetType = 'POST' | 'COMMENT';
export type ReportReason = 'SPAM' | 'ABUSE' | 'OBSCENE' | 'OTHER';

export const createReport = (
    targetType: ReportTargetType,
    targetId: number,
    reason: ReportReason,
    detail?: string
) =>
    axiosInstance
        .post('/api/v1/reports', { targetType, targetId, reason, detail })
        .then(r => r.data.data);
