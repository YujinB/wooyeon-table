package com.wooyeon.table.domain.profile.domain.model;

import java.util.regex.Pattern;

/**
 * MBTI 네 축. 원문 정규화(대소문자·오타·복수 응답)는 입력 파서가 맡고, 여기서는 올바른 4글자 코드만 받는다.
 */
public record Mbti(boolean extrovert, boolean intuitive, boolean thinking, boolean judging) {

    private static final Pattern CODE = Pattern.compile("[EI][NS][TF][JP]");

    public static Mbti of(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("MBTI 코드가 아님: " + code);
        }
        return new Mbti(code.charAt(0) == 'E', code.charAt(1) == 'N', code.charAt(2) == 'T', code.charAt(3) == 'J');
    }

    public String code() {
        return new String(new char[]{
                extrovert ? 'E' : 'I',
                intuitive ? 'N' : 'S',
                thinking ? 'T' : 'F',
                judging ? 'J' : 'P'
        });
    }
}
