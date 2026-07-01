package com.example.sb10_MoPl_team3.tmdb.client;

import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@Tag("external")
class TmdbApiClientIntegrationTest {

    @Autowired
    private TmdbApiClient tmdbApiClient;

    @Value("${tmdb.access-token:}")
    private String accessToken;

    @Test
    @DisplayName("page=1 요청 시 비어 있지 않은 영화 목록과 page=1을 반환한다")
    void getPopularMovies_page1_returnsNonEmptyResultsAndMatchingPage() {
        assumeTrue(!accessToken.isBlank(), "TMDB_ACCESS_TOKEN 미설정 — 테스트를 건너뜁니다.");

        TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(1);

        assertThat(response).isNotNull();
        assertThat(response.results()).isNotEmpty();
        assertThat(response.page()).isEqualTo(1);
    }

    @Test
    @DisplayName("page=2 요청 시 응답의 page 값도 2다")
    void getPopularMovies_page2_reflectsRequestedPageNumber() {
        assumeTrue(!accessToken.isBlank(), "TMDB_ACCESS_TOKEN 미설정 — 테스트를 건너뜁니다.");

        TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(2);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.results()).isNotEmpty();
    }
}