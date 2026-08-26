package org.example.petinside.debug;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.notification.entity.NotificationType;
import org.example.petinside.domain.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;

// TEMP: 좋아요 알림 생성 실패 원인 진단용. 확인 끝나면 삭제할 것.
@RestController
@RequiredArgsConstructor
public class DebugController {

    private final NotificationService notificationService;

    @GetMapping("/api/v1/debug/test-notification")
    public ResponseEntity<String> testNotification(@RequestParam Long userId) {
        try {
            notificationService.createNotification(userId, NotificationType.POST_LIKE,
                    "디버그 테스트 알림", 1L, "/posts/1");
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return ResponseEntity.status(500).body(sw.toString());
        }
    }
}
