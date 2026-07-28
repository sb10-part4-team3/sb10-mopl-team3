package com.example.sb10_MoPl_team3.sportsdb.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.sb10_MoPl_team3.global.exception.SportsDbApiException;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class SportsDbApiClientTest {

    private static final String API_KEY = "SECRET-SPORTSDB-KEY-af92c1";
    private static final String LEAGUE_ID = "4328";

    private WireMockServer wireMockServer;
    private SportsDbApiClient sportsDbApiClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        sportsDbApiClient = buildClient(wireMockServer.baseUrl(), Duration.ofSeconds(3));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("정상 응답을 받으면 JSON 필드가 DTO에 그대로 매핑된다")
    void getNextEventsByLeague_정상_응답_시_DTO_매핑() {
        wireMockServer.stubFor(get(urlPathEqualTo("/" + API_KEY + "/eventsnextleague.php"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "events": [
                        {
                          "idEvent": "1001",
                          "strEvent": "Arsenal vs Chelsea",
                          "dateEvent": "2026-08-01",
                          "strLeague": "English Premier League",
                          "strVenue": "Emirates Stadium",
                          "strThumb": "https://example.com/thumb.jpg"
                        }
                      ]
                    }
                    """)));

        SportsDbEventsResponse response = sportsDbApiClient.getNextEventsByLeague(LEAGUE_ID);

        assertThat(response.events()).hasSize(1);
        SportsDbEventsResponse.SportsDbEvent event = response.events().get(0);
        assertThat(event.idEvent()).isEqualTo("1001");
        assertThat(event.eventName()).isEqualTo("Arsenal vs Chelsea");
        assertThat(event.dateEvent()).isEqualTo("2026-08-01");
        assertThat(event.league()).isEqualTo("English Premier League");
        assertThat(event.venue()).isEqualTo("Emirates Stadium");
        assertThat(event.thumbnail()).isEqualTo("https://example.com/thumb.jpg");
    }

    @Test
    @DisplayName("지난 경기 조회도 정상 응답을 DTO로 매핑한다")
    void getPastEventsByLeague_정상_응답_시_DTO_매핑() {
        wireMockServer.stubFor(get(urlPathEqualTo("/" + API_KEY + "/eventspastleague.php"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(emptyEvents())));

        SportsDbEventsResponse response = sportsDbApiClient.getPastEventsByLeague(LEAGUE_ID);

        assertThat(response.events()).isEmpty();
    }

    @Test
    @DisplayName("4xx 응답을 받으면 SportsDbApiException을 던진다")
    void getNextEventsByLeague_4xx_응답_시_SportsDbApiException() {
        wireMockServer.stubFor(get(urlPathEqualTo("/" + API_KEY + "/eventsnextleague.php"))
            .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> sportsDbApiClient.getNextEventsByLeague(LEAGUE_ID))
            .isInstanceOf(SportsDbApiException.class);
    }

    @Test
    @DisplayName("5xx 응답을 받으면 SportsDbApiException을 던진다")
    void getNextEventsByLeague_5xx_응답_시_SportsDbApiException() {
        wireMockServer.stubFor(get(urlPathEqualTo("/" + API_KEY + "/eventsnextleague.php"))
            .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> sportsDbApiClient.getNextEventsByLeague(LEAGUE_ID))
            .isInstanceOf(SportsDbApiException.class);
    }

    @Test
    @DisplayName("응답이 타임아웃보다 늦게 오면 SportsDbApiException을 던진다")
    void getNextEventsByLeague_타임아웃_시_SportsDbApiException() {
        SportsDbApiClient shortTimeoutClient = buildClient(wireMockServer.baseUrl(), Duration.ofMillis(300));

        wireMockServer.stubFor(get(urlPathEqualTo("/" + API_KEY + "/eventsnextleague.php"))
            .willReturn(aResponse()
                .withFixedDelay(1000)
                .withHeader("Content-Type", "application/json")
                .withBody(emptyEvents())));

        assertThatThrownBy(() -> shortTimeoutClient.getNextEventsByLeague(LEAGUE_ID))
            .isInstanceOf(SportsDbApiException.class)
            .hasCauseInstanceOf(org.springframework.web.client.ResourceAccessException.class);
    }

    @Test
    @DisplayName("API 키 값이 애플리케이션 로그에 노출되지 않는다")
    void getNextEventsByLeague_API키가_로그에_노출되지_않는다() {
        // API 키가 URL 경로에 직접 포함되는 구조라, 요청 URL을 로깅하는 코드가 있으면 그대로 노출될 위험이 있다.
        // 애플리케이션 패키지 로거만 캡처해서(WireMock 자체의 요청 수신 로그는 대상 아님) 실제로 새는지 확인한다.
        Logger appLogger = (Logger) LoggerFactory.getLogger("com.example.sb10_MoPl_team3");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        appLogger.addAppender(appender);
        Level originalLevel = appLogger.getLevel();
        appLogger.setLevel(Level.ALL);

        try {
            wireMockServer.stubFor(get(urlPathEqualTo("/" + API_KEY + "/eventsnextleague.php"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(emptyEvents())));

            sportsDbApiClient.getNextEventsByLeague(LEAGUE_ID);

            boolean keyLeaked = appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(API_KEY));
            assertThat(keyLeaked)
                .as("애플리케이션 로그 메시지에 API 키 값이 그대로 노출되면 안 된다")
                .isFalse();
        } finally {
            appLogger.detachAppender(appender);
            appLogger.setLevel(originalLevel);
        }
    }

    @Test
    @DisplayName("예외 메시지에 API 키 값을 포함하지 않는다")
    void getNextEventsByLeague_예외_메시지에_API키가_노출되지_않는다() {
        wireMockServer.stubFor(get(urlPathEqualTo("/" + API_KEY + "/eventsnextleague.php"))
            .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> sportsDbApiClient.getNextEventsByLeague(LEAGUE_ID))
            .hasMessageNotContaining(API_KEY);
    }

    private SportsDbApiClient buildClient(String baseUrl, Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(readTimeout);
        ClientHttpRequestFactory requestFactory =
            ClientHttpRequestFactoryBuilder.detect().build(settings);

        RestClient restClient = RestClient.builder()
            .baseUrl(baseUrl + "/" + API_KEY)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

        return new SportsDbApiClient(restClient);
    }

    private String emptyEvents() {
        return """
            {"events": []}
            """;
    }
}
