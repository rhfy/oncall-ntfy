package com.example.oncallntfy.model;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 진행 중인 알림의 에스컬레이션 상태 (인메모리) */
public class AlertState {

    public final String alertId;
    public final String message;
    public final List<String> oncallTopics;

    /** 무단 Ack 방지용 1회성 토큰 */
    public final String ackToken = UUID.randomUUID().toString();

    public final AtomicInteger currentIndex = new AtomicInteger(0);
    public final AtomicBoolean acked = new AtomicBoolean(false);

    /** 타임아웃 에스컬레이션 예약 작업 (취소용) */
    public volatile ScheduledFuture<?> escalationTask;

    public AlertState(String alertId, String message, List<String> oncallTopics) {
        this.alertId = alertId;
        this.message = message;
        this.oncallTopics = List.copyOf(oncallTopics);
    }
}
