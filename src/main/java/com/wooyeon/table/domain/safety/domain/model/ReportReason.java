package com.wooyeon.table.domain.safety.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    HARASSMENT("불쾌한 신체 접촉·성희롱"),
    OFFENSIVE_LANGUAGE("욕설·비하 발언"),
    UNWANTED_CONTACT("원치 않는 연락·만남 강요"),
    SOLICITATION("영업·종교·다단계 권유"),
    OTHER("기타");

    private final String label;
}
