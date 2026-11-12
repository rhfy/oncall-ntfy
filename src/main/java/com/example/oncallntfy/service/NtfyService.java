package com.example.oncallntfy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * ntfy 서버로 푸시 알림을 발송한다.
 *
 * <pre>
 * Priority: urgent (5) → Android 방해금지 모드 뚫기
 * Actions: http       → 알림 버튼 클릭 시 Ack 서버로 POST
 * </pre>
 */
@Service
public class NtfyService {

    private static final Logger log = LoggerFactory.getLogger(NtfyService.class);

    private final RestTemplate rest;

    @Value("${ntfy.base-url}")
    private String ntfyBaseUrl;

    @Value("${oncall.ack-base-url}")
    private String ackBaseUrl;

    public NtfyService(RestTemplate rest) {
        this.rest = rest;
    }

    public void sendAlert(String topic, String alertId, String message, String ackToken) {
        var headers = new HttpHeaders();
        headers.set("Title", "🚨 긴급 장애 발생");
        headers.set("Priority", "urgent");
        headers.set("Tags", "rotating_light,sos");
        // ntfy Actions 문법: type, label, url[, method=POST]
        headers.set("Actions", String.format(
                "http, 인지(Ack), %s/ack?alertId=%s&token=%s, method=POST",
                ackBaseUrl, alertId, ackToken
        ));
        headers.setContentType(MediaType.TEXT_PLAIN);

        rest.exchange(
                ntfyBaseUrl + "/" + topic,
                HttpMethod.POST,
                new HttpEntity<>(message, headers),
                Void.class
        );

        log.debug("ntfy 발송 완료 topic={} alertId={}", topic, alertId);
    }
}
