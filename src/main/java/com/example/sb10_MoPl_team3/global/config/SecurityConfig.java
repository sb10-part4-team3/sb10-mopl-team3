package com.example.sb10_MoPl_team3.global.config;

import com.example.sb10_MoPl_team3.global.security.csrf.CsrfCookieFilter;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtAuthenticationFilter;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtSessionValidator;
import com.example.sb10_MoPl_team3.oauth.handler.OAuthLoginFailureHandler;
import com.example.sb10_MoPl_team3.oauth.handler.OAuthLoginSuccessHandler;
import com.example.sb10_MoPl_team3.oauth.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.sb10_MoPl_team3.oauth.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.SessionManagementFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CsrfCookieFilter csrfCookieFilter,
            ObjectProvider<CustomOAuth2UserService> customOAuth2UserServiceProvider,
            ObjectProvider<OAuthLoginSuccessHandler> oAuthLoginSuccessHandlerProvider,
            ObjectProvider<OAuthLoginFailureHandler> oAuthLoginFailureHandlerProvider,
            ObjectProvider<HttpCookieOAuth2AuthorizationRequestRepository> authorizationRequestRepositoryProvider,
            @Value("${oauth.enabled:false}") boolean oauthEnabled
    ) throws Exception {

        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
                new CsrfTokenRequestAttributeHandler();
        csrfTokenRequestHandler.setCsrfRequestAttributeName("_csrf");

        http
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfTokenRequestHandler)
                .ignoringRequestMatchers("/ws/**"))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) ->
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf-token").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/prometheus"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/favicon.svg",
                    "/assets/**"
                ).permitAll()
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // Ensure deferred CSRF tokens are materialized after stateless session handling.
            .addFilterAfter(csrfCookieFilter, SessionManagementFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauthEnabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .authorizedClientRepository(new NoOpOAuth2AuthorizedClientRepository())
                    .authorizationEndpoint(authorization -> authorization
                            .authorizationRequestRepository(authorizationRequestRepositoryProvider.getObject()))
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserServiceProvider.getObject()))
                    .successHandler(oAuthLoginSuccessHandlerProvider.getObject())
                    .failureHandler(oAuthLoginFailureHandlerProvider.getObject())
            );
        }

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtProvider jwtProvider,
            JwtSessionValidator jwtSessionValidator
    ) {
        return new JwtAuthenticationFilter(jwtProvider, jwtSessionValidator);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CsrfCookieFilter csrfCookieFilter() {
        return new CsrfCookieFilter();
    }

    private static class NoOpOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                String clientRegistrationId,
                Authentication principal,
                HttpServletRequest request
        ) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(
                OAuth2AuthorizedClient authorizedClient,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response
        ) {
        }

        @Override
        public void removeAuthorizedClient(
                String clientRegistrationId,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response
        ) {
        }
    }
}
