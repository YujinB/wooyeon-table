package com.wooyeon.table.domain.user.application.dto;

import com.wooyeon.table.domain.user.domain.model.ConsentPolicy.Section;
import java.time.LocalDateTime;
import java.util.List;

/** 내 동의 상태와 동의 내용 (ACC-05). agreedAt은 지금 버전에 동의했을 때만 */
public record MyConsent(boolean agreed, String version, LocalDateTime agreedAt, List<Section> sections) {
}
