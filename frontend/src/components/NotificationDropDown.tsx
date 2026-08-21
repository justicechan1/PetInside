// components/NotificationDropdown.tsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getNotifications, readNotification } from '../api/notificationApi.ts';
import type { NotificationItem } from '../api/notificationApi.ts';

// 💡 1. 여기에 방금 저장한 고양이 이미지 경로를 적어줍니다!
// (파일을 저장한 위치에 따라 '../assets/cat_icon.png' 등으로 수정해주세요)
import catIcon from './cat_icon.png';

export default function NotificationDropdown() {
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const [notifications, setNotifications] = useState<NotificationItem[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);

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
            <div onClick={() => setOpen(!open)} style={{ cursor: 'pointer', position: 'relative', display: 'flex', alignItems: 'center' }}>

                {/* 💡 2. 긴 코드 대신 딱 이 한 줄로 이미지를 띄웁니다 */}
                <img src={catIcon} alt="알림" style={{ width: 28, height: 28, objectFit: 'contain' }} />

                {unreadCount > 0 && (
                    <span style={{
                        position: 'absolute', top: -4, right: -4,
                        background: '#E03131', color: '#fff',
                        borderRadius: '50%', width: 16, height: 16,
                        fontSize: 10, display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontWeight: 'bold'
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