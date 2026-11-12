package com.example.oncallntfy.controller;

import com.example.oncallntfy.service.EscalationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ntfy 알림 버튼("인지(Ack)")의 콜백 수신.
 *
 * ntfy Actions 헤더의 http 타입은 POST를 사용하고,
 * GET은 브라우저에서 URL을 직접 열었을 때를 대비한 fallback이다.
 */
@RestController
@RequestMapping("/ack")
public class AckController {

    private final EscalationService escalation;

    public AckController(EscalationService escalation) {
        this.escalation = escalation;
    }

    @PostMapping
    public ResponseEntity<String> ackPost(@RequestParam String alertId, @RequestParam String token) {
        return handle(alertId, token);
    }

    @GetMapping
    public ResponseEntity<String> ackGet(@RequestParam String alertId, @RequestParam String token) {
        return handle(alertId, token);
    }

    private ResponseEntity<String> handle(String alertId, String token) {
        if (escalation.ack(alertId, token)) {
            return ResponseEntity.ok("✅ 인지 완료. 에스컬레이션이 중단됩니다.");
        }
        // 410 Gone: 이미 처리됐거나 만료된 토큰
        return ResponseEntity.status(HttpStatus.GONE).body("이미 처리되었거나 유효하지 않은 요청입니다.");
    }
}
