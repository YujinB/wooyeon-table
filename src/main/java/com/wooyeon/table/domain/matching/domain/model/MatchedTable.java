package com.wooyeon.table.domain.matching.domain.model;

import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.util.List;

/**
 * 필수 조건 검증과 최소 점수를 통과한 테이블 한 개와 PRD 7.2 표시 항목.
 *
 * @param mbtiComposition 멤버 순서대로의 MBTI 코드, 모르면 "?"
 * @param commonInterests 두 명 이상이 고른 관심사 (많이 겹친 순)
 */
public record MatchedTable(
        int tableNo,
        Region region,
        List<TableMember> members,
        int minAge,
        int maxAge,
        int maleCount,
        int femaleCount,
        List<String> mbtiComposition,
        List<Interest> commonInterests,
        int previouslyMetPairs,
        TableScore score
) {

    public int size() {
        return members.size();
    }

    public int ageGap() {
        return maxAge - minAge;
    }
}
