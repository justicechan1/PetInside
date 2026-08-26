package org.example.petinside.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.example.petinside.domain.notification.entity.PushSubscription;
import org.example.petinside.domain.notification.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.util.List;

@Slf4j
@Service
public class WebPushSender {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushService pushService;

    public WebPushSender(
            PushSubscriptionRepository pushSubscriptionRepository,
            @Value("${webpush.vapid.public-key}") String publicKey,
            @Value("${webpush.vapid.private-key}") String privateKey,
            @Value("${webpush.vapid.subject}") String subject
    ) throws Exception {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        Security.addProvider(new BouncyCastleProvider());
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    public void sendPushToUser(Long userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(userId);

        for (PushSubscription sub : subscriptions) {
            try {
                nl.martijndwars.webpush.Subscription subscription = new nl.martijndwars.webpush.Subscription(
                        sub.getEndpoint(),
                        new nl.martijndwars.webpush.Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );

                String payload = String.format(
                        "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\"}", title, body, url
                );

                Notification notification = new Notification(subscription, payload);
                pushService.send(notification);
            } catch (Exception e) {
                log.warn("푸시 전송 실패 (endpoint: {}): {}", sub.getEndpoint(), e.getMessage());
            }
        }
    }
}