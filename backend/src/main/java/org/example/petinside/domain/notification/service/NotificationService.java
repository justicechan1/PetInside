package org.example.petinside.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.notification.dto.NotificationReadResponse;
import org.example.petinside.domain.notification.dto.NotificationResponse;
import org.example.petinside.domain.notification.entity.Notification;
import org.example.petinside.domain.notification.repository.NotificationRepository;
import org.example.petinside.global.exception.CustomException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private  final NotificationRepository notificationRepository;

    // F-36-1: 알림 목록 조회
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,pageable)
                .map(NotificationResponse::from);
    }

    // F-36-2: 알림 읽음 처리
    @Transactional
    public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "존재하지 않는 알림입니다."));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(HttpStatus.FORBIDDEN.value(), "본인의 알림만 읽음 처리 할 수 있습니다.");
        }

        notification.markAsRead();
        return new NotificationReadResponse(notification.getId());
    }
}
