package com.wooyeon.table.domain.matching.domain.model;

import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자동 확정된 테이블과 PRD 7.2 표시 항목 (MAT-10·12) */
@Getter
@Entity
@Table(name = "dining_tables", indexes = @Index(name = "idx_dining_tables_slot", columnList = "slotId"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiningTable extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long matchRunId;

    @Column(nullable = false)
    private Long slotId;

    /** 회차 안에서의 테이블 번호 */
    private int tableNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiningTableStatus status;

    private int minAge;
    private int maxAge;
    private int maleCount;
    private int femaleCount;

    /** 예: "ENFP,INFJ,?,ISTJ" */
    @Column(nullable = false, length = 100)
    private String mbtiComposition;

    /** 두 명 이상이 고른 관심사, 많이 겹친 순. 예: "FOOD_CAFE,TRAVEL" */
    @Column(nullable = false, length = 300)
    private String commonInterests;

    private int previouslyMetPairs;

    private double totalScore;
    private double genderScore;
    private double mbtiScore;
    private double interestScore;
    private double preferenceScore;
    private double noveltyScore;

    @Column(nullable = false)
    private LocalDateTime confirmedAt;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<TableSeat> seats = new ArrayList<>();

    /**
     * @param userIdByApplication 신청 id → 사용자 id (엔진 후보 id는 신청 id)
     */
    public static DiningTable confirmed(Long matchRunId, Long slotId, MatchedTable matched,
                                        Map<Long, Long> userIdByApplication, LocalDateTime confirmedAt) {
        DiningTable table = new DiningTable();
        table.matchRunId = matchRunId;
        table.slotId = slotId;
        table.tableNo = matched.tableNo();
        table.region = matched.region();
        table.status = DiningTableStatus.CONFIRMED;
        table.minAge = matched.minAge();
        table.maxAge = matched.maxAge();
        table.maleCount = matched.maleCount();
        table.femaleCount = matched.femaleCount();
        table.mbtiComposition = String.join(",", matched.mbtiComposition());
        table.commonInterests = matched.commonInterests().stream().map(Interest::name)
                .collect(Collectors.joining(","));
        table.previouslyMetPairs = matched.previouslyMetPairs();
        TableScore score = matched.score();
        table.totalScore = score.total();
        table.genderScore = score.genderBalance();
        table.mbtiScore = score.mbti();
        table.interestScore = score.interest();
        table.preferenceScore = score.preference();
        table.noveltyScore = score.novelty();
        table.confirmedAt = confirmedAt;
        for (TableMember member : matched.members()) {
            table.seats.add(new TableSeat(table, member, userIdByApplication.get(member.candidateId())));
        }
        return table;
    }

    public int size() {
        return seats.size();
    }
}
