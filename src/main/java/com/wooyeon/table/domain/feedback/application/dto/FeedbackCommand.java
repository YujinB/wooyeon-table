package com.wooyeon.table.domain.feedback.application.dto;

public record FeedbackCommand(int tableMood, int conversation, int venue, int rejoinIntent, int recommendScore,
                              String comment) {
}
