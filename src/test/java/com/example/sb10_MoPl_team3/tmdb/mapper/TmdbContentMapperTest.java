package com.example.sb10_MoPl_team3.tmdb.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse.TmdbMovieResult;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbTvPopularResponse.TmdbTvResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TmdbContentMapperTest {

    private final TmdbContentMapper mapper = new TmdbContentMapper();

    @Test
    @DisplayName("영화 응답을 Content로 변환하면 필드가 올바르게 매핑된다")
    void toContent_영화_정상_변환() {
        TmdbMovieResult movie = new TmdbMovieResult(
            550L,
            "파이트 클럽",
            "Fight Club",
            "설명입니다",
            "/poster.jpg",
            "/backdrop.jpg",
            "1999-10-15",
            61.4,
            8.4,
            26280,
            List.of(18, 53),
            false
        );

        Content content = mapper.toContent(movie);

        assertThat(content.getType()).isEqualTo(ContentType.MOVIE);
        assertThat(content.getTitle()).isEqualTo("파이트 클럽");
        assertThat(content.getDescription()).isEqualTo("설명입니다");
        assertThat(content.getThumbnailUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(content.getExternalId()).isEqualTo("MOVIE-550");
        assertThat(content.getSource()).isEqualTo("TMDB");
    }

    @Test
    @DisplayName("posterPath가 없는 영화는 thumbnailUrl이 null이다")
    void toContent_영화_posterPath_없으면_thumbnailUrl_null() {
        TmdbMovieResult movie = new TmdbMovieResult(
            551L, "제목", "Title", "설명", null, "/backdrop.jpg",
            "2020-01-01", 1.0, 5.0, 10, List.of(), false
        );

        Content content = mapper.toContent(movie);

        assertThat(content.getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("TV 응답을 Content로 변환하면 필드가 올바르게 매핑된다")
    void toContent_TV_정상_변환() {
        TmdbTvResult tv = new TmdbTvResult(
            1399L,
            "왕좌의 게임",
            "Game of Thrones",
            "설명입니다",
            "/tv-poster.jpg",
            "/tv-backdrop.jpg",
            "2011-04-17",
            50.0,
            8.4,
            21000,
            List.of(18, 10765),
            List.of("US")
        );

        Content content = mapper.toContent(tv);

        assertThat(content.getType()).isEqualTo(ContentType.TV_SERIES);
        assertThat(content.getTitle()).isEqualTo("왕좌의 게임");
        assertThat(content.getDescription()).isEqualTo("설명입니다");
        assertThat(content.getThumbnailUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/tv-poster.jpg");
        assertThat(content.getExternalId()).isEqualTo("TV-1399");
        assertThat(content.getSource()).isEqualTo("TMDB");
    }

    @Test
    @DisplayName("posterPath가 빈 문자열인 TV는 thumbnailUrl이 null이다")
    void toContent_TV_posterPath_빈문자열이면_thumbnailUrl_null() {
        TmdbTvResult tv = new TmdbTvResult(
            1400L, "제목", "Title", "설명", "", "/backdrop.jpg",
            "2020-01-01", 1.0, 5.0, 10, List.of(), List.of()
        );

        Content content = mapper.toContent(tv);

        assertThat(content.getThumbnailUrl()).isNull();
    }
}
