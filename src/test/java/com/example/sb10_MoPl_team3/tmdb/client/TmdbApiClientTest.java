package com.example.sb10_MoPl_team3.tmdb.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.sb10_MoPl_team3.global.exception.TmdbApiException;
import com.example.sb10_MoPl_team3.tmdb.dto.TmdbMoviePopularResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class TmdbApiClientTest {

    private static final String ACCESS_TOKEN = "SECRET-TEST-TOKEN-af92c1";

    private WireMockServer wireMockServer;
    private TmdbApiClient tmdbApiClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        tmdbApiClient = buildClient(wireMockServer.baseUrl(), Duration.ofSeconds(3));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("정상 응답을 받으면 JSON 필드가 DTO에 그대로 매핑된다")
    void getPopularMovies_정상_응답_시_DTO_매핑() {
        wireMockServer.stubFor(get(urlPathEqualTo("/movie/popular"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "page": 1,
                      "results": [
                        {
                          "id": 550,
                          "title": "파이트 클럽",
                          "original_title": "Fight Club",
                          "overview": "설명입니다",
                          "poster_path": "/poster.jpg",
                          "backdrop_path": "/backdrop.jpg",
                          "release_date": "1999-10-15",
                          "popularity": 61.4,
                          "vote_average": 8.4,
                          "vote_count": 26280,
                          "genre_ids": [18, 53],
                          "adult": false
                        }
                      ],
                      "total_pages": 5,
                      "total_results": 100
                    }
                    """)));

        TmdbMoviePopularResponse response = tmdbApiClient.getPopularMovies(1);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(5);
        assertThat(response.totalResults()).isEqualTo(100);
        assertThat(response.results()).hasSize(1);

        TmdbMoviePopularResponse.TmdbMovieResult result = response.results().get(0);
        assertThat(result.id()).isEqualTo(550L);
        assertThat(result.title()).isEqualTo("파이트 클럽");
        assertThat(result.originalTitle()).isEqualTo("Fight Club");
        assertThat(result.overview()).isEqualTo("설명입니다");
        assertThat(result.posterPath()).isEqualTo("/poster.jpg");
        assertThat(result.backdropPath()).isEqualTo("/backdrop.jpg");
        assertThat(result.releaseDate()).isEqualTo("1999-10-15");
        assertThat(result.voteAverage()).isEqualTo(8.4);
        assertThat(result.voteCount()).isEqualTo(26280);
        assertThat(result.genreIds()).containsExactly(18, 53);
        assertThat(result.adult()).isFalse();
    }

    @Test
    @DisplayName("요청에 Authorization 헤더로 액세스 토큰이 전달된다")
    void getPopularMovies_요청에_Authorization_헤더가_포함된다() {
        wireMockServer.stubFor(get(urlPathEqualTo("/movie/popular"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(emptyMoviePage())));

        tmdbApiClient.getPopularMovies(1);

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/movie/popular"))
            .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
    }

    @Test
    @DisplayName("4xx 응답을 받으면 TmdbApiException을 던진다")
    void getPopularMovies_4xx_응답_시_TmdbApiException() {
        wireMockServer.stubFor(get(urlPathEqualTo("/movie/popular"))
            .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> tmdbApiClient.getPopularMovies(1))
            .isInstanceOf(TmdbApiException.class);
    }

    @Test
    @DisplayName("5xx 응답을 받으면 TmdbApiException을 던진다")
    void getPopularMovies_5xx_응답_시_TmdbApiException() {
        wireMockServer.stubFor(get(urlPathEqualTo("/movie/popular"))
            .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> tmdbApiClient.getPopularMovies(1))
            .isInstanceOf(TmdbApiException.class);
    }

    @Test
    @DisplayName("응답이 타임아웃보다 늦게 오면 예외를 던진다")
    void getPopularMovies_타임아웃_시_예외() {
        TmdbApiClient shortTimeoutClient = buildClient(wireMockServer.baseUrl(), Duration.ofMillis(300));

        wireMockServer.stubFor(get(urlPathEqualTo("/movie/popular"))
            .willReturn(aResponse()
                .withFixedDelay(1000)
                .withHeader("Content-Type", "application/json")
                .withBody(emptyMoviePage())));

        assertThatThrownBy(() -> shortTimeoutClient.getPopularMovies(1))
            .isInstanceOf(TmdbApiException.class)
            .hasCauseInstanceOf(org.springframework.web.client.ResourceAccessException.class);
    }

    @Test
    @DisplayName("액세스 토큰 값이 애플리케이션 로그에 노출되지 않는다")
    void getPopularMovies_액세스토큰이_로그에_노출되지_않는다() {
        // WireMock 서버 자체의 요청 수신 로그(테스트 목 서버 내부 로그)는 검증 대상이 아니므로
        // 애플리케이션 패키지 로거로만 범위를 좁혀서, 우리 코드/스프링 클라이언트가 토큰을 로깅하는지만 확인한다.
        Logger appLogger = (Logger) LoggerFactory.getLogger("com.example.sb10_MoPl_team3");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        appLogger.addAppender(appender);
        ch.qos.logback.classic.Level originalLevel = appLogger.getLevel();
        appLogger.setLevel(ch.qos.logback.classic.Level.ALL);

        try {
            wireMockServer.stubFor(get(urlPathEqualTo("/movie/popular"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(emptyMoviePage())));

            tmdbApiClient.getPopularMovies(1);

            boolean tokenLeaked = appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(ACCESS_TOKEN));
            assertThat(tokenLeaked)
                .as("애플리케이션 로그 메시지에 액세스 토큰 값이 그대로 노출되면 안 된다")
                .isFalse();
        } finally {
            appLogger.detachAppender(appender);
            appLogger.setLevel(originalLevel);
        }
    }

    @Test
    @DisplayName("토큰이 설정되지 않으면 예외 메시지에 토큰 값을 포함하지 않는다")
    void validateAccessToken_미설정_시_예외_메시지에_토큰_노출_없음() {
        TmdbApiClient noTokenClient = buildClient(wireMockServer.baseUrl(), Duration.ofSeconds(3));
        ReflectionTestUtils.setField(noTokenClient, "accessToken", "");

        assertThatThrownBy(() -> noTokenClient.getPopularMovies(1))
            .isInstanceOf(TmdbApiException.class)
            .hasMessageNotContaining(ACCESS_TOKEN);
    }

    private TmdbApiClient buildClient(String baseUrl, Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(readTimeout);
        ClientHttpRequestFactory requestFactory =
            ClientHttpRequestFactoryBuilder.detect().build(settings);

        RestClient restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("Authorization", "Bearer " + ACCESS_TOKEN)
            .defaultHeader("Accept", "application/json")
            .build();

        TmdbApiClient client = new TmdbApiClient(restClient);
        ReflectionTestUtils.setField(client, "accessToken", ACCESS_TOKEN);
        return client;
    }

    private String emptyMoviePage() {
        return """
            {"page": 1, "results": [], "total_pages": 1, "total_results": 0}
            """;
    }
}
