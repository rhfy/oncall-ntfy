package com.example.oncallntfy.controller;

import com.example.oncallntfy.model.AlertRequest;
import com.example.oncallntfy.service.EscalationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모니터링 시스템(Prometheus Alertmanager, Grafana Webhook 등) 연동 진입점.
 *
 * <pre>
 * POST /alert
 * {
 *   "alertId": "db-conn-fail-001",
 *   "message": "DB 연결 오류: connection refused (10.0.0.5:3306)",
 *   "oncallTopics": ["oncall-hong-gildong", "oncall-kim-cheolsu"]
 * }
 * </pre>
 */
@RestController
@RequestMapping("/alert")
public class AlertController {

    private final EscalationService escalation;

    public AlertController(EscalationService escalation) {
        this.escalation = escalation;
    }

    @PostMapping
    public ResponseEntity<String> trigger(@RequestBody AlertRequest req) {
        escalation.start(req);
        return ResponseEntity.ok("escalation started: " + req.alertId());
    }
}
