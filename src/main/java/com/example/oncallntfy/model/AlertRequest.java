package com.example.oncallntfy.model;

import java.util.List;

/**
 * 모니터링 시스템이 POST /alert 로 보내는 페이로드.
 *
 * oncallTopics: 에스컬레이션 순서대로 나열된 ntfy 토픽 목록
 *   - 토픽은 담당자별로 고유하게 사전 약속 (예: "oncall-hong-gildong")
 *   - ntfy 앱에서 해당 토픽을 구독해야 알림이 수신됨
 */
public record AlertRequest(
        String alertId,
        String message,
        List<String> oncallTopics
) {}
