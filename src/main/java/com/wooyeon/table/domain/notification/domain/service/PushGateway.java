package com.wooyeon.table.domain.notification.domain.service;

/** 앱 푸시 발송 포트. 해커톤은 모의 구현 (실제 FCM 등으로 바꿀 때 구현만 교체) */
public interface PushGateway {

    /** 실패하면 예외 메시지를 담은 RuntimeException */
    void send(Long userId, String title, String body);
}
