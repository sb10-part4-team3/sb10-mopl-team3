package com.example.sb10_MoPl_team3.global.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.example.sb10_MoPl_team3.notification.dto.NotificationDto;
import com.example.sb10_MoPl_team3.notification.enums.NotificationLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedisSseConnectionRepositoryTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ListOperations<String, String> listOperations;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    RedisSseConnectionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RedisSseConnectionRepository(redisTemplate, objectMapper);
        ReflectionTestUtils.setField(repository, "maxEventCacheSize", 100);
    }

    @Test
    void saveEvent_storesJsonPayloadAndTrimsLatestEvents() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        given(redisTemplate.opsForList()).willReturn(listOperations);

        repository.saveEvent(userId, SseEventCache.of(
                "event-1",
                "notifications",
                notificationDto(notificationId)));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        then(listOperations).should().rightPush(
                org.mockito.ArgumentMatchers.eq(key(userId)),
                payloadCaptor.capture());
        then(listOperations).should().trim(key(userId), -100, -1);
        SseEventCachePayload payload =
                objectMapper.readValue(payloadCaptor.getValue(), SseEventCachePayload.class);
        assertThat(payload.id()).isEqualTo("event-1");
        assertThat(payload.name()).isEqualTo("notifications");
        assertThat(payload.data().get("id").asText()).isEqualTo(notificationId.toString());
    }

    @Test
    void findCachedEventsAfter_replaysEventsAfterLastEventId() throws Exception {
        UUID userId = UUID.randomUUID();
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(key(userId), 0, -1)).willReturn(List.of(
                payload("event-1", "notifications", notificationDto(UUID.randomUUID())),
                payload("event-2", "notifications", notificationDto(UUID.randomUUID())),
                payload("event-3", "direct-messages", notificationDto(UUID.randomUUID()))));

        assertThat(repository.findCachedEventsAfter(userId, "event-1"))
                .extracting(SseEventCache::id)
                .containsExactly("event-2", "event-3");
    }

    @Test
    void findCachedEventsByUserId_returnsEmptyListWhenRedisListIsMissing() {
        UUID userId = UUID.randomUUID();
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(key(userId), 0, -1)).willReturn(null);

        assertThat(repository.findCachedEventsByUserId(userId)).isEmpty();
    }

    @Test
    void findCachedEventsAfter_replaysAllEventsWhenLastEventIdIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(key(userId), 0, -1)).willReturn(List.of(
                payload("event-1", "notifications", notificationDto(UUID.randomUUID())),
                payload("event-2", "notifications", notificationDto(UUID.randomUUID()))));

        assertThat(repository.findCachedEventsAfter(userId, "unknown-event"))
                .extracting(SseEventCache::id)
                .containsExactly("event-1", "event-2");
    }

    @Test
    void findCachedEventsByUserId_ignoresCorruptedPayloads() throws Exception {
        UUID userId = UUID.randomUUID();
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(key(userId), 0, -1)).willReturn(List.of(
                "invalid-json",
                payload("event-1", "notifications", notificationDto(UUID.randomUUID()))));

        assertThat(repository.findCachedEventsByUserId(userId))
                .extracting(SseEventCache::id)
                .containsExactly("event-1");
    }

    @Test
    void deleteCachedEvents_removesOnlyPredicateMatchedPayloads() throws Exception {
        UUID userId = UUID.randomUUID();
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(key(userId), 0, -1)).willReturn(List.of(
                payload("event-1", "notifications", notificationDto(UUID.randomUUID())),
                payload("event-2", "direct-messages", notificationDto(UUID.randomUUID())),
                payload("event-3", "notifications", notificationDto(UUID.randomUUID()))));

        repository.deleteCachedEvents(userId, event -> "notifications".equals(event.name()));

        ArgumentCaptor<String> removedPayloadCaptor = ArgumentCaptor.forClass(String.class);
        then(listOperations).should(times(2)).remove(
                org.mockito.ArgumentMatchers.eq(key(userId)),
                org.mockito.ArgumentMatchers.eq(1L),
                removedPayloadCaptor.capture());
        assertThat(removedPayloadCaptor.getAllValues())
                .extracting(this::payloadId)
                .containsExactly("event-1", "event-3");
    }

    @Test
    void deleteCachedEventByDataId_removesOnlyMatchingNotificationEvent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID otherNotificationId = UUID.randomUUID();
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.range(key(userId), 0, -1)).willReturn(List.of(
                payload("event-1", "notifications", notificationDto(notificationId)),
                payload("event-2", "notifications", notificationDto(otherNotificationId)),
                payload("event-3", "direct-messages", notificationDto(notificationId))));

        repository.deleteCachedEventByDataId(userId, "notifications", notificationId);

        ArgumentCaptor<String> removedPayloadCaptor = ArgumentCaptor.forClass(String.class);
        then(listOperations).should(times(1)).remove(
                org.mockito.ArgumentMatchers.eq(key(userId)),
                org.mockito.ArgumentMatchers.eq(1L),
                removedPayloadCaptor.capture());
        assertThat(removedPayloadCaptor.getAllValues())
                .extracting(this::payloadId)
                .containsExactly("event-1");
    }

    @Test
    void findEmittersByUserId_usesLocalJvmStorage() {
        UUID userId = UUID.randomUUID();
        var emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();

        String emitterId = repository.saveEmitter(userId, emitter);

        assertThat(repository.findEmittersByUserId(userId))
                .containsEntry(emitterId, emitter);
    }

    @Test
    void deleteAllCachedEvents_deletesRedisEventCacheKey() {
        UUID userId = UUID.randomUUID();

        repository.deleteAllCachedEvents(userId);

        then(redisTemplate).should().delete(key(userId));
    }

    private String payload(String id, String name, Object data) throws Exception {
        SseEventCache event = SseEventCache.of(id, name, data);
        return objectMapper.writeValueAsString(
                SseEventCachePayload.from(event, objectMapper.valueToTree(data)));
    }

    private NotificationDto notificationDto(UUID id) {
        return new NotificationDto(
                id,
                Instant.parse("2026-06-24T00:00:00Z"),
                UUID.randomUUID(),
                "제목",
                "내용",
                NotificationLevel.INFO);
    }

    private String payloadId(String value) {
        try {
            return objectMapper.readValue(value, SseEventCachePayload.class).id();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private String key(UUID userId) {
        return "sse:users:%s:events".formatted(userId);
    }
}
