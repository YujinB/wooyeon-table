package com.wooyeon.table.domain.feedback.application.dto;

import java.util.List;

/**
 * 운영자용 회차 후기 요약 (FDB-02·03). 사람별 선호(FDB-04)는 비공개라 담지 않는다.
 *
 * @param nps 추천 의향 순추천지수: 9~10점 비율 - 0~6점 비율 (%)
 */
public record SlotFeedbackSummary(Long slotId, int responses, Averages averages, int nps, List<TableFeedback> tables,
                                  List<Comment> comments) {

    public record Averages(double tableMood, double conversation, double venue, double rejoinIntent,
                           double recommendScore) {
    }

    public record TableFeedback(Long tableId, int responses, Averages averages) {
    }

    public record Comment(Long tableId, String name, int tableMood, String comment) {
    }
}
