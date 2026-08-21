package org.example.petinside.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.notification.dto.NotificationReadResponse;
import org.example.petinside.domain.notification.dto.NotificationResponse;
import org.example.petinside.domain.notification.dto.PushSubscribeRequest;
import org.example.petinside.domain.notification.entity.Notification;
import org.example.petinside.domain.notification.entity.NotificationType;
import org.example.petinside.domain.notification.entity.PushSubscription;
import org.example.petinside.domain.notification.repository.NotificationRepository;
import org.example.petinside.domain.notification.repository.PushSubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private  final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushSender webPushSender;

    // F-34-1: 알림 목록 조회
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,pageable)
                .map(NotificationResponse::from);
    }

    // F-34-2: 알림 읽음 처리
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

    // F-34-3: 알림 생성 기능
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(Long receiverUserId, NotificationType type,String content, Long targetId, String linkUrl) {
       User receiver = userRepository.findById(receiverUserId)
               .orElseThrow(() -> new UserNotFoundException(receiverUserId));

        Notification notification = Notification.builder()
                .user(receiver)
                .type(type)
                .content(content)
                .targetId(targetId)
                .linkUrl(linkUrl)
                .build();
        notificationRepository.save(notification);

        // 웹푸시 전송
        webPushSender.sendPushToUser(receiverUserId, "PetInside 알림", content, linkUrl);
    }

    // F-34-4: 웹푸시 구독 저장
    @Transactional
    public void savePushSubscription(Long userId, PushSubscribeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 이미 같은 endpoint로 구독돼 있으면 중복 저장 방지
        pushSubscriptionRepository.findByEndpoint(request.endpoint())
                .ifPresentOrElse(
                        existing -> {}, // 이미 있으면 아무것도 안 함
                        () -> {
                            PushSubscription subscription = new PushSubscription(
                                    user.getId(),
                                    request.endpoint(),
                                    request.keys().p256dh(),
                                    request.keys().auth()
                            );
                            pushSubscriptionRepository.save(subscription);
                        }
                );
    }
}
