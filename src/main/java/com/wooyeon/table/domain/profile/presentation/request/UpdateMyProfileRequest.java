package com.wooyeon.table.domain.profile.presentation.request;

import com.wooyeon.table.domain.profile.application.dto.MyProfileCommand;
import com.wooyeon.table.domain.profile.domain.model.Gender;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.MealBudget;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.PreferredTrait;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Set;

public record UpdateMyProfileRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대전화 번호 형식이 아닙니다") String phone,
        @NotNull @Min(1940) @Max(2008) Integer birthYear,
        @NotNull Gender gender,
        @Size(max = 100) String job,
        @Pattern(regexp = "^[EIei][NSns][TFtf][JPjp]$", message = "MBTI 4글자 코드가 아닙니다") String mbti,
        @NotEmpty Set<Interest> interests,
        @NotEmpty Set<Trait> traits,
        @NotEmpty Set<PreferredTrait> preferredTraits,
        @NotEmpty Set<MealBudget> mealBudgets,
        @Size(max = 500) String foodRestriction) {

    public MyProfileCommand toCommand() {
        Mbti parsed = mbti == null || mbti.isBlank() ? null : Mbti.of(mbti.toUpperCase(Locale.ROOT));
        return new MyProfileCommand(name.strip(), normalizePhone(phone), birthYear, gender, blankToNull(job), parsed,
                interests, traits, preferredTraits, mealBudgets, blankToNull(foodRestriction));
    }

    /** 하이픈 유무와 상관없이 010-1234-5678(10자리는 011-123-4567) 꼴로 저장 */
    static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        int middle = digits.length() == 10 ? 6 : 7;
        return digits.substring(0, 3) + "-" + digits.substring(3, middle) + "-" + digits.substring(middle);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
