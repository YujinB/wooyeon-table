package com.wooyeon.table.domain.application.infrastructure.excel;

import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 신청자 엑셀 한 줄을 정규화한 값.
 *
 * @param rowNumber        엑셀 행 번호 (1부터)
 * @param mbti             해석할 수 없으면 null
 * @param regions          서울 4개 지역만. 비어 있으면 서울 밖만 고른 사람
 * @param rawRegion        지역 원문 (서울 밖 지역 확인용)
 * @param foodRestriction  못 먹는 음식 원문. 없음 계열 답변은 빈 문자열
 */
public record ApplicantRow(
        int rowNumber,
        String name,
        int age,
        Gender gender,
        Mbti mbti,
        Set<Region> regions,
        String rawRegion,
        Set<Interest> interests,
        Set<Trait> traits,
        Set<PreferredTrait> preferredTraits,
        Set<MealBudget> mealBudgets,
        List<LocalDate> availableDates,
        TicketType ticketType,
        String foodRestriction
) {
}
