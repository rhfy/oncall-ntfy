package com.example.oncallntfy.service;

import com.example.oncallntfy.model.AlertRequest;
import com.example.oncallntfy.model.AlertState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 에스컬레이션 흐름 관리.
 * - 인메모리 상태: 재시작 시 진행 중인 에스컬레이션 소멸 (shortcut: 단일 인스턴스 가정, 재시작 복원 필요 시 Redis로 교체)
 * - ScheduledExecutorService로 타임아웃 에스컬레이션을 처리한다.
 */
@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final NtfyService ntfy;
    private final int timeoutSeconds;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<String, AlertState> states = new ConcurrentHashMap<>();

    public EscalationService(
            NtfyService ntfy,
            @Value("${oncall.escalation-timeout-seconds:300}") int timeoutSeconds) {
        this.ntfy = ntfy;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** 모니터링 시스템으로부터 장애 알림을 받아 에스컬레이션 시작 */
    public void start(AlertRequest req) {
        var state = new AlertState(req.alertId(), req.message(), req.oncallTopics());
        states.put(req.alertId(), state);
        sendAndSchedule(state);
    }

    /**
     * 담당자가 Ack 버튼을 눌렀을 때 호출.
     *
     * @return 유효한 Ack이면 true, 토큰 불일치 또는 이미 처리됨이면 false
     */
    public boolean ack(String alertId, String token) {
        AlertState state = states.get(alertId);
        if (state == null || !state.ackToken.equals(token)) return false;
        if (!state.acked.compareAndSet(false, true)) return false; // 중복 Ack 방지

        if (state.escalationTask != null) state.escalationTask.cancel(false);
        states.remove(alertId);
        log.info("[{}] ACK 완료. 에스컬레이션 중단.", alertId);
        return true;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ── private ────────────────────────────────────────────────────────────────

    private void sendAndSchedule(AlertState state) {
        int idx = state.currentIndex.get();
        if (idx >= state.oncallTopics.size()) {
            log.warn("[{}] 모든 담당자 소진. 미처리 상태.", state.alertId);
            states.remove(state.alertId);
            return;
        }

        String topic = state.oncallTopics.get(idx);
        try {
            ntfy.sendAlert(topic, state.alertId, state.message, state.ackToken);
            log.info("[{}] 알림 발송 → {} ({}번째 / 총 {}명)",
                    state.alertId, topic, idx + 1, state.oncallTopics.size());
        } catch (Exception e) {
            log.error("[{}] ntfy 발송 실패: {}", state.alertId, e.getMessage());
        }

        // 타임아웃 후 자동 에스컬레이션
        state.escalationTask = scheduler.schedule(() -> {
            if (state.acked.get()) return;
            log.info("[{}] {}초 무응답 → 다음 담당자로 에스컬레이션", state.alertId, timeoutSeconds);
            state.currentIndex.incrementAndGet();
            sendAndSchedule(state);
        }, timeoutSeconds, TimeUnit.SECONDS);
    }
}
