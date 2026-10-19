package com.wooyeon.table.domain.venue.application;

import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.venue.domain.model.Venue;
import com.wooyeon.table.domain.venue.domain.repository.VenueRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데모용 파트너 식당 재고. 실제 식당이 아닌 가상 식당이며(주최 측 자료에 식당 데이터 없음),
 * 지도 링크는 동네(역) 검색으로 연결한다. 식당이 하나도 없을 때만 넣는다
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class VenueSeeder implements ApplicationRunner {

    private static final String MAP_SEARCH = "https://map.naver.com/p/search/";

    private final VenueRepository venueRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (venueRepository.count() > 0) {
            return;
        }
        List<Venue> venues = List.of(
                venue("[가상] 합정 담소식당", Region.HONGDAE_HAPJEONG, "서울 마포구 합정역 인근 (가상)", "합정역",
                        MealBudget.FROM_20K_TO_30K, "1인 2~3만원 (코스)", 3, true),
                venue("[가상] 연남 한상", Region.HONGDAE_HAPJEONG, "서울 마포구 홍대입구역 인근 (가상)", "홍대입구역",
                        MealBudget.FROM_10K_TO_20K, "1인 1.5~2만원", 2, true),
                venue("[가상] 상수 와인키친", Region.HONGDAE_HAPJEONG, "서울 마포구 상수역 인근 (가상)", "상수역",
                        MealBudget.OVER_30K, "1인 3~4만원 (와인 별도)", 2, false),
                venue("[가상] 신논현 테이블", Region.GANGNAM_SINNONHYEON, "서울 강남구 신논현역 인근 (가상)", "신논현역",
                        MealBudget.FROM_20K_TO_30K, "1인 2.5~3만원", 3, true),
                venue("[가상] 강남 화로", Region.GANGNAM_SINNONHYEON, "서울 강남구 강남역 인근 (가상)", "강남역",
                        MealBudget.OVER_30K, "1인 3.5만원 내외", 2, true),
                venue("[가상] 역삼 밥집", Region.GANGNAM_SINNONHYEON, "서울 강남구 역삼역 인근 (가상)", "역삼역",
                        MealBudget.FROM_10K_TO_20K, "1인 1.5~2만원", 2, true),
                venue("[가상] 을지로 노포", Region.EULJIRO_JONGNO, "서울 중구 을지로3가역 인근 (가상)", "을지로3가역",
                        MealBudget.FROM_10K_TO_20K, "1인 1.5~2만원", 3, true),
                venue("[가상] 종로 한옥다이닝", Region.EULJIRO_JONGNO, "서울 종로구 종로3가역 인근 (가상)", "종로3가역",
                        MealBudget.FROM_20K_TO_30K, "1인 2~3만원", 2, true),
                venue("[가상] 잠실 레이크키친", Region.JAMSIL, "서울 송파구 잠실역 인근 (가상)", "잠실역",
                        MealBudget.FROM_20K_TO_30K, "1인 2~3만원", 3, true),
                venue("[가상] 송리단길 비스트로", Region.JAMSIL, "서울 송파구 송리단길 인근 (가상)", "송리단길",
                        MealBudget.OVER_30K, "1인 3~4만원", 2, true));
        venues.forEach(venueRepository::save);
        log.info("데모 파트너 식당 {}곳 등록", venues.size());
    }

    private static Venue venue(String name, Region region, String address, String mapKeyword, MealBudget budget,
                               String priceGuide, int tablesPerSlot, boolean autoBookable) {
        return Venue.builder()
                .name(name)
                .region(region)
                .address(address)
                .mapUrl(MAP_SEARCH + URLEncoder.encode(mapKeyword, StandardCharsets.UTF_8))
                .mealBudget(budget)
                .priceGuide(priceGuide)
                .tablesPerSlot(tablesPerSlot)
                .autoBookable(autoBookable)
                .build();
    }
}
