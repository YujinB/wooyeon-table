package com.wooyeon.table.domain.application.infrastructure.excel;

import static org.assertj.core.api.Assertions.assertThat;

import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.Region;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApplicantExcelParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "isfp|ISFP",
            "intj/infj|INTJ",
            "INFJ 요즘은 INTJ|INFJ",
            "E(I)NFP|ENFP",
            "lstp |ISTP",
            "wNTJ|ENTJ",
            "ISTP인데 다 반반이에요..|ISTP"
    })
    void MBTI는_대문자로_정규화하고_처음_나온_유형을_쓴다(String raw, String expected) {
        assertThat(ApplicantExcelParser.parseMbti(raw)).isEqualTo(Mbti.of(expected));
    }

    @Test
    void MBTI를_알아볼_수_없으면_null() {
        assertThat(ApplicantExcelParser.parseMbti("모름")).isNull();
    }

    @Test
    void 지역은_키워드로_매핑하고_서울_밖은_버린다() {
        assertThat(ApplicantExcelParser.parseRegions("홍대/합정, 을지로/종로"))
                .containsExactlyInAnyOrder(Region.HONGDAE_HAPJEONG, Region.EULJIRO_JONGNO);
        assertThat(ApplicantExcelParser.parseRegions("분당이나 강남..잠실근처"))
                .containsExactlyInAnyOrder(Region.GANGNAM_SINNONHYEON, Region.JAMSIL);
        assertThat(ApplicantExcelParser.parseRegions("홍대 또는 부평")).containsExactly(Region.HONGDAE_HAPJEONG);
        assertThat(ApplicantExcelParser.parseRegions("수원 인계동")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"서울권 모두 상관없습니다", "지역 무방합니다."})
    void 상관없음_계열은_4개_지역_전부(String raw) {
        assertThat(ApplicantExcelParser.parseRegions(raw)).containsExactlyInAnyOrder(Region.values());
    }

    @Test
    void 관심사는_쉼표가_든_선택지도_하나로_읽는다() {
        assertThat(ApplicantExcelParser.parseLabels("운동 (헬스, 러닝, 클라이밍 등), 맛집·카페 탐방, 여행",
                Interest.class, Interest::getLabel))
                .containsExactlyInAnyOrder(Interest.EXERCISE, Interest.FOOD_CAFE, Interest.TRAVEL);
    }

    @Test
    void 날짜는_2026년으로_읽고_중복_없이_정렬한다() {
        assertThat(ApplicantExcelParser.parseDates("8/6 (목), 7/30 (목), 8/20(목)"))
                .containsExactly(LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 20));
        assertThat(ApplicantExcelParser.parseDates("9/10 (목) (홍대, 강남만 가능!)"))
                .containsExactly(LocalDate.of(2026, 9, 10));
    }

    @Test
    void 이용권은_1회권만_단건이고_나머지는_다회권() {
        assertThat(ApplicantExcelParser.parseTicket("1회권: ₩8,000")).isEqualTo(TicketType.SINGLE);
        assertThat(ApplicantExcelParser.parseTicket("3회권 사용 (3회권 결제자 분들만)")).isEqualTo(TicketType.MULTI);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "없음", "없습니다!", "없어요!", "X", "x", "아니요", "아뇨", "다 잘먹습니다!"})
    void 못_먹는_음식_없음_계열은_빈_값(String raw) {
        assertThat(ApplicantExcelParser.parseFoodRestriction(raw)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"고수", "없음(해산물불호..)", "음식은 없지만 논알콜 선호합니다..!"})
    void 못_먹는_음식_정보가_있으면_원문을_둔다(String raw) {
        assertThat(ApplicantExcelParser.parseFoodRestriction(raw)).isEqualTo(raw);
    }
}
