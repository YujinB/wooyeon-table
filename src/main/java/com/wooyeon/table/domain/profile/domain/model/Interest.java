package com.wooyeon.table.domain.profile.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Interest {
    MOVIE_DRAMA("영화·드라마"),
    MUSIC("음악"),
    READING_WRITING("독서·글쓰기"),
    EXERCISE("운동 (헬스, 러닝, 클라이밍 등)"),
    FOOD_CAFE("맛집·카페 탐방"),
    TRAVEL("여행"),
    EXHIBITION_ART("전시·공연·아트"),
    FASHION_BEAUTY("패션·뷰티"),
    SELF_DEVELOPMENT("자기계발·공부"),
    PHOTO_VIDEO("사진·영상");

    private final String label;
}
