package com.wooyeon.table.domain.chat.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator.Icebreaker;
import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator.MemberSignal;
import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator.TableContent;
import com.wooyeon.table.domain.chat.domain.service.TableContentGenerator.Topic;
import com.wooyeon.table.domain.profile.domain.model.Interest;
import com.wooyeon.table.domain.profile.domain.model.Mbti;
import com.wooyeon.table.domain.profile.domain.model.Trait;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableContentGeneratorTest {

    @Test
    void 모두의_공통_관심사가_첫_주제이고_2명_이상_관심사가_다음_그다음_테이블_성향_주제() {
        List<MemberSignal> members = List.of(
                member(Set.of(Interest.TRAVEL, Interest.MUSIC), Set.of(Trait.DEEP_TALK), "INFJ"),
                member(Set.of(Interest.TRAVEL, Interest.MUSIC), Set.of(Trait.DEEP_TALK), "ENFP"),
                member(Set.of(Interest.TRAVEL, Interest.EXERCISE), Set.of(Trait.CALM), "ISTJ"),
                member(Set.of(Interest.TRAVEL), Set.of(Trait.DEEP_TALK), null));

        List<Topic> topics = TableContentGenerator.generate(members).topics();

        assertThat(topics).hasSize(3);
        assertThat(topics.get(0).question()).contains("여행");
        assertThat(topics.get(0).basis()).isEqualTo("모두 '여행'을(를) 좋아해요");
        assertThat(topics.get(1).basis()).isEqualTo("2명이 '음악'을(를) 골랐어요");
        assertThat(topics.get(2).basis()).isEqualTo("깊은 대화를 좋아하는 분이 많아요");
    }

    @Test
    void 겹치는_관심사가_없어도_기본_주제로_최소_3개() {
        List<MemberSignal> members = List.of(
                member(Set.of(Interest.TRAVEL), Set.of(), null),
                member(Set.of(Interest.MUSIC), Set.of(), null),
                member(Set.of(Interest.EXERCISE), Set.of(), null),
                member(Set.of(Interest.PHOTO_VIDEO), Set.of(), null));

        List<Topic> topics = TableContentGenerator.generate(members).topics();

        assertThat(topics).hasSize(TableContentGenerator.MIN_TOPICS)
                .allSatisfy(topic -> assertThat(topic.basis()).contains("누구나 편하게"));
    }

    @Test
    void 관심사가_많이_겹쳐도_주제는_최대_5개() {
        Set<Interest> many = Set.of(Interest.TRAVEL, Interest.MUSIC, Interest.FOOD_CAFE, Interest.MOVIE_DRAMA,
                Interest.EXHIBITION_ART, Interest.READING_WRITING);
        List<MemberSignal> members = List.of(member(many, Set.of(Trait.LIGHT_HUMOR), "ENFP"),
                member(many, Set.of(Trait.LIGHT_HUMOR), "INFP"), member(many, Set.of(), "ESTP"),
                member(many, Set.of(), "ISFJ"));

        assertThat(TableContentGenerator.generate(members).topics()).hasSize(TableContentGenerator.MAX_TOPICS);
    }

    @Test
    void 아이스브레이킹은_많은_대화_성향에_맞는_게임과_MBTI_공개_여부에_따른_게임() {
        List<MemberSignal> energetic = List.of(
                member(Set.of(), Set.of(Trait.ENERGETIC), "ENFP"), member(Set.of(), Set.of(Trait.LEADS_TALK), "ESTJ"),
                member(Set.of(), Set.of(Trait.ENERGETIC), null), member(Set.of(), Set.of(Trait.CALM), null));
        List<MemberSignal> calmNoMbti = List.of(
                member(Set.of(), Set.of(Trait.CALM), null), member(Set.of(), Set.of(Trait.GOOD_LISTENER), null),
                member(Set.of(), Set.of(Trait.CALM), null), member(Set.of(), Set.of(), "INTP"));

        assertThat(TableContentGenerator.generate(energetic).icebreakers()).extracting(Icebreaker::name)
                .containsExactly("밸런스 게임", "MBTI 맞혀 보기");
        assertThat(TableContentGenerator.generate(calmNoMbti).icebreakers()).extracting(Icebreaker::name)
                .containsExactly("오늘의 한 단어", "공통점 찾기");
    }

    @Test
    void 같은_멤버면_같은_결과() {
        List<MemberSignal> members = List.of(
                member(Set.of(Interest.TRAVEL, Interest.FOOD_CAFE), Set.of(Trait.LIGHT_HUMOR), "ENFP"),
                member(Set.of(Interest.FOOD_CAFE, Interest.TRAVEL), Set.of(Trait.GOOD_LISTENER), "INFJ"));

        TableContent first = TableContentGenerator.generate(members);
        TableContent second = TableContentGenerator.generate(List.of(members.get(1), members.get(0)));

        assertThat(second).isEqualTo(first);
    }

    private static MemberSignal member(Set<Interest> interests, Set<Trait> traits, String mbti) {
        return new MemberSignal(interests, traits, mbti == null ? null : Mbti.of(mbti));
    }
}
