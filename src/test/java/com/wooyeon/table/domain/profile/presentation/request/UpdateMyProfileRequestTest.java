package com.wooyeon.table.domain.profile.presentation.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UpdateMyProfileRequestTest {

    @Test
    void 휴대전화는_하이픈_유무와_상관없이_같은_꼴로_저장한다() {
        assertThat(UpdateMyProfileRequest.normalizePhone("01012345678")).isEqualTo("010-1234-5678");
        assertThat(UpdateMyProfileRequest.normalizePhone("010-1234-5678")).isEqualTo("010-1234-5678");
        assertThat(UpdateMyProfileRequest.normalizePhone("0111234567")).isEqualTo("011-123-4567");
    }
}
