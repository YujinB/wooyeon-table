package com.wooyeon.table.domain.feedback.application.dto;

import java.util.Set;

/** wantAgainSeats·avoidSeats: 단체방 좌석 번호(참가자 N) — 다시 만나고 싶은/싶지 않은 사람 (FDB-04, 비공개) */
public record FeedbackCommand(int tableMood, int conversation, int venue, int rejoinIntent, int recommendScore,
                              String comment, Set<Integer> wantAgainSeats, Set<Integer> avoidSeats) {
}
