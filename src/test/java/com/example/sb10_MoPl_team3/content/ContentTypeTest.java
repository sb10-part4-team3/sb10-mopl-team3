package com.example.sb10_MoPl_team3.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ContentTypeTest {

    @Test
    void fromValue_camelCase값을_허용한다() {
        assertThat(ContentType.fromValue("movie")).isEqualTo(ContentType.MOVIE);
        assertThat(ContentType.fromValue("tvSeries")).isEqualTo(ContentType.TV_SERIES);
        assertThat(ContentType.fromValue("sport")).isEqualTo(ContentType.SPORT);
    }

    @Test
    void fromValue_enum_상수명을_대소문자_구분없이_허용한다() {
        assertThat(ContentType.fromValue("TV_SERIES")).isEqualTo(ContentType.TV_SERIES);
        assertThat(ContentType.fromValue("tv_series")).isEqualTo(ContentType.TV_SERIES);
        assertThat(ContentType.fromValue("MOVIE")).isEqualTo(ContentType.MOVIE);
        assertThat(ContentType.fromValue("SPORT")).isEqualTo(ContentType.SPORT);
    }

    @Test
    void fromValue_null이거나_공백이면_null을_반환한다() {
        assertThat(ContentType.fromValue(null)).isNull();
        assertThat(ContentType.fromValue("")).isNull();
        assertThat(ContentType.fromValue("   ")).isNull();
    }

    @Test
    void fromValue_알수없는_값이면_예외() {
        assertThatThrownBy(() -> ContentType.fromValue("aaa"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
