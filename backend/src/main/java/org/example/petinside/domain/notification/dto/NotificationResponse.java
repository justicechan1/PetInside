package org.example.petinside.domain.notification.dto;

import org.example.petinside.domain.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String content,
        Long targetId,
        boolean isRead,
        String linkUrl,
        LocalDateTime createAt

) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getContent(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getLinkUrl(),
                notification.getCreatedAt()
        );
    }
}
