import type { CSSProperties } from 'react';

export const cardStyle: CSSProperties = {
    background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)', overflow: 'hidden',
};

export const solidBtn: CSSProperties = {
    padding: '8px 18px', border: 'none', borderRadius: 8, whiteSpace: 'nowrap',
    background: 'var(--primary, #FF8C00)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 13,
};

export const outlineBtn: CSSProperties = {
    padding: '8px 18px', border: '1px solid #dee2e6', borderRadius: 8, whiteSpace: 'nowrap',
    background: '#fff', cursor: 'pointer', fontSize: 13,
};

export const inputStyle: CSSProperties = {
    padding: '11px 14px', border: '1px solid #e0e0e0', borderRadius: 8,
    fontSize: 14, width: '100%', boxSizing: 'border-box', background: '#fff',
};
