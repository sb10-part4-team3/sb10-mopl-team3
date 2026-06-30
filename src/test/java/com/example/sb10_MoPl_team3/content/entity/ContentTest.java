package com.example.sb10_MoPl_team3.content.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ContentTest {

    @Test
    void 빌더_type이_null이면_예외() {
        assertThatThrownBy(() ->
            Content.builder()
                .title("제목")
                .externalId("ext-001")
                .source("MANUAL")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("type은 필수입니다");
    }

    @Test
    void 빌더_title이_null이면_예외() {
        assertThatThrownBy(() ->
            Content.builder()
                .type(ContentType.MOVIE)
                .externalId("ext-001")
                .source("MANUAL")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("title은 필수입니다");
    }

    @Test
    void 빌더_title이_공백이면_예외() {
        assertThatThrownBy(() ->
            Content.builder()
                .type(ContentType.MOVIE)
                .title("   ")
                .externalId("ext-001")
                .source("MANUAL")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("title은 필수입니다");
    }

    @Test
    void 빌더_externalId가_null이면_예외() {
        assertThatThrownBy(() ->
            Content.builder()
                .type(ContentType.MOVIE)
                .title("제목")
                .source("MANUAL")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("externalId는 필수입니다");
    }

    @Test
    void 빌더_externalId가_공백이면_예외() {
        assertThatThrownBy(() ->
            Content.builder()
                .type(ContentType.MOVIE)
                .title("제목")
                .externalId("   ")
                .source("MANUAL")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("externalId는 필수입니다");
    }

    @Test
    void 빌더_source가_null이면_예외() {
        assertThatThrownBy(() ->
            Content.builder()
                .type(ContentType.MOVIE)
                .title("제목")
                .externalId("ext-001")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("source는 필수입니다");
    }

    @Test
    void 빌더_source가_공백이면_예외() {
        assertThatThrownBy(() ->
            Content.builder()
                .type(ContentType.MOVIE)
                .title("제목")
                .externalId("ext-001")
                .source("")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("source는 필수입니다");
    }

    @Test
    void 빌더_필수값으로_정상_생성된다() {
        Content content = Content.builder()
            .type(ContentType.TV_SERIES)
            .title("테스트 드라마")
            .externalId("ext-002")
            .source("MANUAL")
            .build();

        assertThat(content.getType()).isEqualTo(ContentType.TV_SERIES);
        assertThat(content.getTitle()).isEqualTo("테스트 드라마");
        assertThat(content.getExternalId()).isEqualTo("ext-002");
        assertThat(content.getSource()).isEqualTo("MANUAL");
        assertThat(content.getDescription()).isNull();
        assertThat(content.getThumbnailUrl()).isNull();
    }

    @Test
    void 빌더_선택값_포함_정상_생성된다() {
        Content content = Content.builder()
            .type(ContentType.MOVIE)
            .title("테스트 영화")
            .description("영화 설명")
            .thumbnailUrl("https://example.com/thumb.jpg")
            .externalId("ext-003")
            .source("TMDB")
            .build();

        assertThat(content.getDescription()).isEqualTo("영화 설명");
        assertThat(content.getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
    }

    @Test
    void update_title이_공백이면_예외() {
        Content content = Content.builder()
            .type(ContentType.MOVIE)
            .title("원래 제목")
            .externalId("ext-001")
            .source("MANUAL")
            .build();

        assertThatThrownBy(() -> content.update("   ", null))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
                assertThat(be.getMessage()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
            });
    }

    @Test
    void update_title이_null이면_기존_title_유지() {
        Content content = Content.builder()
            .type(ContentType.MOVIE)
            .title("원래 제목")
            .externalId("ext-001")
            .source("MANUAL")
            .build();

        content.update(null, null);

        assertThat(content.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    void update_유효한_title로_변경된다() {
        Content content = Content.builder()
            .type(ContentType.MOVIE)
            .title("원래 제목")
            .externalId("ext-001")
            .source("MANUAL")
            .build();

        content.update("새로운 제목", null);

        assertThat(content.getTitle()).isEqualTo("새로운 제목");
    }

    @Test
    void update_description이_변경된다() {
        Content content = Content.builder()
            .type(ContentType.MOVIE)
            .title("제목")
            .externalId("ext-001")
            .source("MANUAL")
            .build();

        content.update(null, "새로운 설명");

        assertThat(content.getDescription()).isEqualTo("새로운 설명");
    }

    @Test
    void update_description이_null이면_기존_description_유지() {
        Content content = Content.builder()
            .type(ContentType.MOVIE)
            .title("제목")
            .description("기존 설명")
            .externalId("ext-001")
            .source("MANUAL")
            .build();

        content.update(null, null);

        assertThat(content.getDescription()).isEqualTo("기존 설명");
    }
}
