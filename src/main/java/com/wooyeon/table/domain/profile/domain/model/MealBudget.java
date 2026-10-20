package com.wooyeon.table.domain.profile.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealBudget {
    FROM_10K_TO_20K("10,000~20,000원"),
    FROM_20K_TO_30K("20,000~30,000원"),
    OVER_30K("30,000원 이상");

    private final String label;
}
