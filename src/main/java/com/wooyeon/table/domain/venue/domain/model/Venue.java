package com.wooyeon.table.domain.venue.domain.model;

import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트너 식당 재고 (PRD 13절 Venue). 회차(목요일 저녁)마다 tablesPerSlot개 테이블(4~6명)을 받을 수 있다.
 * autoBookable=false면 자동 예약이 거절되는 식당(전화 예약만)이라 다음 후보로 넘어간다
 */
@Getter
@Entity
@Table(name = "venues")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Region region;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 500)
    private String mapUrl;

    /** 1인 식사 가격대 (참가자 희망 예산과 맞춰 고른다) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MealBudget mealBudget;

    /** 화면에 보여줄 가격 안내 */
    @Column(nullable = false, length = 100)
    private String priceGuide;

    @Column(nullable = false)
    private int tablesPerSlot;

    @Column(nullable = false)
    private boolean autoBookable;

    @Column(nullable = false)
    private boolean active;

    @Builder
    private Venue(String name, Region region, String address, String mapUrl, MealBudget mealBudget,
                  String priceGuide, int tablesPerSlot, boolean autoBookable) {
        this.name = name;
        this.region = region;
        this.address = address;
        this.mapUrl = mapUrl;
        this.mealBudget = mealBudget;
        this.priceGuide = priceGuide;
        this.tablesPerSlot = tablesPerSlot;
        this.autoBookable = autoBookable;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
