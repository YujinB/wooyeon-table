package com.wooyeon.table.domain.feedback.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모임 후기 (FDB-02·03). 신청당 한 번. 테이블 분위기·대화·식당 1~5점, 재참여 의향 1~5점, 추천 의향 0~10점
 */
@Getter
@Entity
@Table(name = "feedbacks", indexes = @Index(name = "idx_feedbacks_table", columnList = "tableId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long tableId;

    @Column(nullable = false)
    private Long slotId;

    @Column(nullable = false)
    private int tableMood;

    @Column(nullable = false)
    private int conversation;

    @Column(nullable = false)
    private int venue;

    @Column(nullable = false)
    private int rejoinIntent;

    @Column(nullable = false)
    private int recommendScore;

    @Column(length = 500)
    private String comment;

    @Builder
    private Feedback(Long applicationId, Long userId, Long tableId, Long slotId, int tableMood, int conversation,
                     int venue, int rejoinIntent, int recommendScore, String comment) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.tableId = tableId;
        this.slotId = slotId;
        this.tableMood = tableMood;
        this.conversation = conversation;
        this.venue = venue;
        this.rejoinIntent = rejoinIntent;
        this.recommendScore = recommendScore;
        this.comment = comment;
    }
}
