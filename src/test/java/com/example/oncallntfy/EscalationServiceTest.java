package com.example.oncallntfy;

import com.example.oncallntfy.model.AlertRequest;
import com.example.oncallntfy.service.EscalationService;
import com.example.oncallntfy.service.NtfyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock
    NtfyService ntfy;

    EscalationService svc;

    @AfterEach
    void tearDown() {
        if (svc != null) svc.shutdown();
    }

    @Test
    void escalatesToNextOnTimeout() throws InterruptedException {
        svc = new EscalationService(ntfy, 1); // 1초 타임아웃
        svc.start(new AlertRequest("a1", "DB 오류", List.of("topic1", "topic2")));

        Thread.sleep(1500);

        // topic1 발송 후 타임아웃 → topic2 발송
        verify(ntfy, times(2)).sendAlert(any(), any(), any(), any());
    }

    @Test
    void ackStopsEscalation() throws Exception {
        svc = new EscalationService(ntfy, 1);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        svc.start(new AlertRequest("a1", "DB 오류", List.of("topic1", "topic2")));
        verify(ntfy).sendAlert(eq("topic1"), any(), any(), tokenCaptor.capture());

        // 올바른 토큰으로 Ack
        assertTrue(svc.ack("a1", tokenCaptor.getValue()));

        Thread.sleep(1500);

        // Ack 이후 topic2 발송 없음
        verify(ntfy, times(1)).sendAlert(any(), any(), any(), any());
    }

    @Test
    void invalidTokenReturnsFalse() {
        svc = new EscalationService(ntfy, 300);
        svc.start(new AlertRequest("a1", "msg", List.of("topic1")));

        assertFalse(svc.ack("a1", "wrong-token"));
        assertFalse(svc.ack("unknown", "any-token"));
    }
}
