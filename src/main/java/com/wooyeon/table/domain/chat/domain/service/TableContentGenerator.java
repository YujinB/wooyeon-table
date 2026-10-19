package com.wooyeon.table.domain.chat.domain.service;

import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 테이블 맞춤 대화 주제·아이스브레이킹 (CON-01·02). Spring 의존 없는 순수 로직이고 무작위가 없어 같은 테이블이면 같은 결과.
 * 주제 고르는 순서: ① 모두의 공통 관심사 ② 2명 이상이 고른 관심사(많은 순) ③ 테이블 대화 성향 ④ 기본 주제 — 3~5개.
 * 주제마다 고른 이유(basis)를 같이 준다
 */
public final class TableContentGenerator {

    public static final int MIN_TOPICS = 3;
    public static final int MAX_TOPICS = 5;

    private static final Map<Interest, String> INTEREST_TOPICS = new EnumMap<>(Map.of(
            Interest.MOVIE_DRAMA, "최근에 끝까지 몰아 본 영화·드라마와 그 이유는?",
            Interest.MUSIC, "요즘 플레이리스트에서 가장 많이 듣는 노래 한 곡씩 소개해 볼까요?",
            Interest.READING_WRITING, "인생 책 한 권, 또는 최근 밑줄 그은 문장이 있다면?",
            Interest.EXERCISE, "요즘 하는 운동과, 처음 시작하게 된 계기는?",
            Interest.FOOD_CAFE, "최근 다녀온 곳 중 '여기는 꼭 가 봐야 한다' 싶은 맛집·카페는?",
            Interest.TRAVEL, "가장 기억에 남는 여행지와, 다음에 꼭 가고 싶은 곳은?",
            Interest.EXHIBITION_ART, "최근 본 전시·공연 중 가장 좋았던 것은?",
            Interest.FASHION_BEAUTY, "요즘 꽂힌 스타일이나 아이템이 있나요?",
            Interest.SELF_DEVELOPMENT, "올해 새로 배우기 시작한 것, 또는 배우고 싶은 것은?",
            Interest.PHOTO_VIDEO, "휴대폰 사진첩에서 가장 아끼는 사진 한 장을 보여 준다면?"));

    private static final List<String> DEFAULT_TOPICS = List.of(
            "오늘 이 모임에 신청하게 된 계기는?",
            "요즘 가장 소소하게 행복했던 순간은?",
            "평일 저녁이나 주말을 보내는 나만의 방법은?");

    private TableContentGenerator() {
    }

    /** 멤버 한 명의 콘텐츠 재료 */
    public record MemberSignal(Set<Interest> interests, Set<Trait> traits, Mbti mbti) {
    }

    public record Topic(String question, String basis) {
    }

    public record Icebreaker(String name, String howTo, String basis) {
    }

    public record TableContent(List<Topic> topics, List<Icebreaker> icebreakers) {
    }

    public static TableContent generate(Collection<MemberSignal> members) {
        return new TableContent(topics(members), icebreakers(members));
    }

    static List<Topic> topics(Collection<MemberSignal> members) {
        Map<Interest, Long> interestCounts = count(members.stream().map(MemberSignal::interests).toList(),
                Interest.class);
        List<Topic> topics = new ArrayList<>();
        interestCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .sorted(Map.Entry.<Interest, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_TOPICS - 1)
                .forEach(entry -> {
                    String basis = entry.getValue() == members.size()
                            ? "모두 '" + entry.getKey().getLabel() + "'을(를) 좋아해요"
                            : entry.getValue() + "명이 '" + entry.getKey().getLabel() + "'을(를) 골랐어요";
                    topics.add(new Topic(INTEREST_TOPICS.get(entry.getKey()), basis));
                });

        dominantTrait(members).map(TableContentGenerator::toneTopic).ifPresent(topics::add);

        for (String question : DEFAULT_TOPICS) {
            if (topics.size() >= MIN_TOPICS) {
                break;
            }
            topics.add(new Topic(question, "처음 만난 자리에서 누구나 편하게 답할 수 있는 질문이에요"));
        }
        return topics.stream().limit(MAX_TOPICS).toList();
    }

    static List<Icebreaker> icebreakers(Collection<MemberSignal> members) {
        List<Icebreaker> games = new ArrayList<>();
        Trait dominant = dominantTrait(members).orElse(Trait.LIGHT_HUMOR);
        games.add(switch (dominant) {
            case ENERGETIC, LEADS_TALK -> new Icebreaker("밸런스 게임",
                    "한 사람이 둘 중 하나를 고르는 질문을 내고, 모두 동시에 손가락으로 답한 뒤 이유를 한 마디씩 말해요.",
                    "활기차고 대화를 이끄는 분이 많은 테이블이에요");
            case DEEP_TALK -> new Icebreaker("나를 만든 한 장면",
                    "지금의 나에게 영향을 준 영화·책·경험 한 장면을 1분씩 소개해요.",
                    "깊은 대화를 좋아하는 분이 많은 테이블이에요");
            case CALM, GOOD_LISTENER -> new Icebreaker("오늘의 한 단어",
                    "요즘 나를 표현하는 단어 하나와 그 이유를 돌아가며 말하고, 다른 사람이 질문을 하나씩 해요.",
                    "잔잔하게 이야기를 나누고 잘 들어주는 분이 많은 테이블이에요");
            case LIGHT_HUMOR -> new Icebreaker("두 개의 진실, 하나의 거짓말",
                    "나에 대한 문장 세 개 중 거짓말 하나를 섞어 말하면, 나머지가 거짓말을 맞혀요.",
                    "가볍고 유머 있는 대화를 좋아하는 분이 많은 테이블이에요");
        });
        long withMbti = members.stream().map(MemberSignal::mbti).filter(Objects::nonNull).count();
        if (withMbti * 2 >= members.size()) {
            games.add(new Icebreaker("MBTI 맞혀 보기",
                    "자기소개를 한 뒤 서로의 MBTI를 한 글자씩 추리해 보고, 정답을 공개해요.",
                    members.size() + "명 중 " + withMbti + "명이 MBTI를 알려 줬어요"));
        } else {
            games.add(new Icebreaker("공통점 찾기",
                    "5분 안에 테이블 모두에게 해당하는 공통점 세 가지를 찾아요. (관심사·출신·취향 무엇이든)",
                    "서로를 빠르게 알아가기 좋은 게임이에요"));
        }
        return games;
    }

    private static Optional<Trait> dominantTrait(Collection<MemberSignal> members) {
        Map<Trait, Long> counts = count(members.stream().map(MemberSignal::traits).toList(), Trait.class);
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.<Trait, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey);
    }

    private static Topic toneTopic(Trait trait) {
        return switch (trait) {
            case DEEP_TALK -> new Topic("10년 전의 나에게 해 주고 싶은 말이 있다면?",
                    "깊은 대화를 좋아하는 분이 많아요");
            case LIGHT_HUMOR -> new Topic("최근에 겪은 웃픈 에피소드 하나씩!", "가볍고 유머 있는 대화를 좋아하는 분이 많아요");
            case CALM, GOOD_LISTENER -> new Topic("요즘 마음이 편해지는 나만의 시간은?",
                    "잔잔하고 이야기를 잘 들어주는 분이 많아요");
            case ENERGETIC, LEADS_TALK -> new Topic("올해 안에 꼭 해 보고 싶은 도전 하나는?",
                    "활기차고 대화를 이끄는 분이 많아요");
        };
    }

    private static <E extends Enum<E>> Map<E, Long> count(List<Set<E>> sets, Class<E> type) {
        Map<E, Long> counts = new EnumMap<>(type);
        sets.forEach(set -> set.forEach(value -> counts.merge(value, 1L, Long::sum)));
        return counts;
    }
}
