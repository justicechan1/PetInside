// components/NotificationDropdown.tsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getNotifications, readNotification } from '../api/notificationApi.ts';
import type { NotificationItem } from '../api/notificationApi.ts';

export default function NotificationDropdown() {
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const [notifications, setNotifications] = useState<NotificationItem[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);

    // 귀여운 치즈냥이 아이콘 컴포넌트
    const CatIcon = () => (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="26" height="26">
            {/* 귀 */}
            <polygon points="4,15 2,5 9,10" fill="#F6A355"/>
            <polygon points="20,15 22,5 15,10" fill="#F6A355"/>
            <polygon points="4.5,13.5 3.5,7.5 8,11" fill="#FCDCB8"/>
            <polygon points="19.5,13.5 20.5,7.5 16,11" fill="#FCDCB8"/>
            {/* 얼굴 */}
            <path d="M12 8c-4.418 0-8 3.134-8 7s3.582 7 8 7 8-3.134 8-7-3.582-7-8-7z" fill="#F6A355"/>
            <path d="M4 15c0-3.866 3.582-7 8-7s8 3.134 8 7" fill="#F6A355"/>
            <path d="M8 8.5c2.5 0 5.5 3.5 5.5 6.5H10.5C10.5 12 9 9.5 8 8.5z" fill="#FFFFFF"/>
            <path d="M16 8.5c-2.5 0-5.5 3.5-5.5 6.5h3c0-3 1.5-5.5 2.5-6.5z" fill="#FFFFFF"/>
            {/* 눈 */}
            <circle cx="8.5" cy="14.5" r="1.5" fill="#4B3E3D"/>
            <circle cx="15.5" cy="14.5" r="1.5" fill="#4B3E3D"/>
            {/* 코와 입 */}
            <path d="M12 16.5l-1-1h2z" fill="#E87A90"/>
            <path d="M10 18.5a2 2 0 0 0 4 0" stroke="#4B3E3D" strokeWidth="1" fill="none"/>
        </svg>
    );

    useEffect(() => {
        loadNotifications();
    }, []);

    const loadNotifications = () => {
        getNotifications(0).then(data => {
            setNotifications(data.content);
            setUnreadCount(data.content.filter(n => !n.isRead).length);
        });
    };

    const handleClick = async (notification: NotificationItem) => {
        if (!notification.isRead) {
            await readNotification(notification.id);
            loadNotifications();
        }
        setOpen(false);
        navigate(notification.linkUrl);
    };

    return (
        <div style={{ position: 'relative' }}>
            <div onClick={() => setOpen(!open)} style={{ cursor: 'pointer', position: 'relative', fontSize: 22 }}>

                {/* 🔔 자리에 CatIcon을 렌더링합니다 */}
                <CatIcon />

                {unreadCount > 0 && (
                    <span style={{
                        position: 'absolute', top: -4, right: -4,
                        background: '#E03131', color: '#fff',
                        borderRadius: '50%', width: 16, height: 16,
                        fontSize: 10, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                        {unreadCount}
                    </span>
                )}
            </div>

            {open && (
                <div style={{
                    position: 'absolute', right: 0, top: 36,
                    background: '#fff', border: '1px solid #eee',
                    borderRadius: 8, boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                    width: 300, maxHeight: 400, overflowY: 'auto', zIndex: 100,
                }}>
                    {notifications.length === 0 ? (
                        <div style={{ padding: 20, textAlign: 'center', color: '#999', fontSize: 13 }}>
                            알림이 없습니다
                        </div>
                    ) : (
                        notifications.map(n => (
                            <div key={n.id} onClick={() => handleClick(n)} style={{
                                padding: '12px 16px', cursor: 'pointer',
                                borderBottom: '1px solid #f0f0f0',
                                background: n.isRead ? '#fff' : '#f0f7ff',
                                fontSize: 13,
                            }}>
                                <div>{n.content}</div>
                                <div style={{ fontSize: 11, color: '#999', marginTop: 4 }}>
                                    {new Date(n.createdAt).toLocaleString()}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            )}
        </div>
    );
}