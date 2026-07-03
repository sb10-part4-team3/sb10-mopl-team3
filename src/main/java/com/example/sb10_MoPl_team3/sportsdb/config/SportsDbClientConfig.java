package com.example.sb10_MoPl_team3.sportsdb.config;

import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class SportsDbClientConfig {

  private final SportsDbProperties sportsDbProperties;

  public SportsDbClientConfig(SportsDbProperties sportsDbProperties) {
    this.sportsDbProperties = sportsDbProperties;
  }

  @Bean
  public RestClient sportsDbRestClient() {
    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
        .withConnectTimeout(Duration.ofSeconds(3))
        .withReadTimeout(Duration.ofSeconds(5));

    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactoryBuilder.detect().build(settings);

    return RestClient.builder()
        .baseUrl(sportsDbProperties.getBaseUrl() + "/" + sportsDbProperties.getApiKey())
        .requestFactory(requestFactory)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}