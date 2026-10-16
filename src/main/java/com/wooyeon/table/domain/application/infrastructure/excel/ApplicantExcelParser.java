package com.wooyeon.table.domain.application.infrastructure.excel;

import com.wooyeon.table.domain.application.domain.model.TicketType;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Region;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * 주최 측 신청자 엑셀(가상데이터) 파서. 첫 행은 비어 있고 두 번째 행이 헤더다.
 * 연락처·직업·타임스탬프·인스타 열은 비어 있어 읽지 않는다.
 */
public final class ApplicantExcelParser {

    private static final int HEADER_ROW = 1;
    private static final int COL_NAME = 1;
    private static final int COL_AGE = 4;
    private static final int COL_GENDER = 5;
    private static final int COL_MBTI = 7;
    private static final int COL_REGION = 8;
    private static final int COL_INTEREST = 9;
    private static final int COL_TRAIT = 10;
    private static final int COL_PREFERRED = 11;
    private static final int COL_BUDGET = 12;
    private static final int COL_DATES = 13;
    private static final int COL_TICKET = 14;
    private static final int COL_FOOD = 16;

    /** 날짜는 모두 2026년 목요일 "M/D (목)" 형식 */
    private static final int DATA_YEAR = 2026;
    private static final Pattern DATE = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})");
    /** 오타 보정: W는 E 옆 키, L은 I와 모양이 같아 보정한다 (wNTJ, Wsfp, lstp) */
    private static final Pattern MBTI = Pattern.compile("[EIWL][NS][TF][JP]");
    private static final Pattern PARENTHESIZED = Pattern.compile("\\([^)]*\\)");
    private static final List<String> ANY_REGION_KEYWORDS = List.of("상관없", "상관 없", "무방", "모두");
    private static final Pattern NO_FOOD_RESTRICTION =
            Pattern.compile("^(없.{0,3}|아니.{0,2}|아뇨|x|다 잘 ?먹.*)$", Pattern.CASE_INSENSITIVE);

    public List<ApplicantRow> parse(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ApplicantRow> rows = new ArrayList<>();
            for (int i = HEADER_ROW + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || text(row, COL_AGE).isEmpty()) {
                    continue;
                }
                rows.add(toApplicant(i + 1, row));
            }
            return rows;
        }
    }

    private ApplicantRow toApplicant(int rowNumber, Row row) {
        String rawRegion = text(row, COL_REGION);
        return new ApplicantRow(
                rowNumber,
                text(row, COL_NAME),
                (int) Double.parseDouble(text(row, COL_AGE)),
                Gender.fromLabel(text(row, COL_GENDER)),
                parseMbti(text(row, COL_MBTI)),
                parseRegions(rawRegion),
                rawRegion,
                parseLabels(text(row, COL_INTEREST), Interest.class, Interest::getLabel),
                parseLabels(text(row, COL_TRAIT), Trait.class, Trait::getLabel),
                parseLabels(text(row, COL_PREFERRED), PreferredTrait.class, PreferredTrait::getLabel),
                parseLabels(text(row, COL_BUDGET), MealBudget.class, MealBudget::getLabel),
                parseDates(text(row, COL_DATES)),
                parseTicket(text(row, COL_TICKET)),
                parseFoodRestriction(text(row, COL_FOOD)));
    }

    /** 대문자로 바꾸고 괄호 안을 지운 뒤 처음 나오는 MBTI 코드를 쓴다 ("intj/infj" → INTJ, "E(I)NFP" → ENFP) */
    public static Mbti parseMbti(String raw) {
        String normalized = PARENTHESIZED.matcher(raw.toUpperCase(Locale.ROOT)).replaceAll("");
        Matcher matcher = MBTI.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        String code = matcher.group().replace('W', 'E').replace('L', 'I');
        return Mbti.of(code);
    }

    /** 키워드로 4개 지역에 매핑. "상관없음/무방/모두"는 4곳 전부, 서울 밖 지역은 버린다 */
    public static Set<Region> parseRegions(String raw) {
        for (String keyword : ANY_REGION_KEYWORDS) {
            if (raw.contains(keyword)) {
                return EnumSet.allOf(Region.class);
            }
        }
        Set<Region> regions = EnumSet.noneOf(Region.class);
        for (Region region : Region.values()) {
            if (region.getKeywords().stream().anyMatch(raw::contains)) {
                regions.add(region);
            }
        }
        return regions;
    }

    /** 선택지 라벨에 쉼표가 들어 있는 경우가 있어("운동 (헬스, 러닝, 클라이밍 등)") 쉼표로 자르지 않고 라벨 포함 여부로 판단한다 */
    public static <E extends Enum<E>> Set<E> parseLabels(String raw, Class<E> type,
                                                         Function<E, String> label) {
        Set<E> result = EnumSet.noneOf(type);
        for (E option : type.getEnumConstants()) {
            if (raw.contains(label.apply(option))) {
                result.add(option);
            }
        }
        return result;
    }

    public static List<LocalDate> parseDates(String raw) {
        Set<LocalDate> dates = new TreeSet<>();
        Matcher matcher = DATE.matcher(raw);
        while (matcher.find()) {
            dates.add(LocalDate.of(DATA_YEAR, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
        }
        return List.copyOf(dates);
    }

    public static TicketType parseTicket(String raw) {
        return raw.startsWith("1회권") ? TicketType.SINGLE : TicketType.MULTI;
    }

    /** "없음/없습니다/X/아니요" 같은 짧은 답은 빈 값. 그 외는 원문 그대로 둔다 ("없음(해산물불호..)"처럼 정보가 붙은 답 포함) */
    public static String parseFoodRestriction(String raw) {
        String trimmed = raw.strip();
        String withoutPunctuation = trimmed.replaceAll("[.!~ ]+$", "");
        if (withoutPunctuation.isEmpty() || NO_FOOD_RESTRICTION.matcher(withoutPunctuation).matches()) {
            return "";
        }
        return trimmed;
    }

    private static String text(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return cell.toString().strip();
    }
}
