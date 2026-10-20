package com.wooyeon.table.domain.profile.domain.model;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Region {
    HONGDAE_HAPJEONG("홍대/합정", List.of("홍대", "합정")),
    GANGNAM_SINNONHYEON("강남/신논현", List.of("강남", "신논현")),
    EULJIRO_JONGNO("을지로/종로", List.of("을지로", "종로")),
    JAMSIL("잠실", List.of("잠실"));

    private final String label;
    /** 자유 입력에서 이 지역으로 볼 키워드 */
    private final List<String> keywords;
}
