package org.example.petinside.domain.notification.dto;

import io.jsonwebtoken.security.Keys;

public record PushSubscribeRequest(
        String endpoint,
        Keys keys
) {
    public record Keys(String p256dh, String auth) {}
}
