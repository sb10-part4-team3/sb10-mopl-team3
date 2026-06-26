package com.example.sb10_MoPl_team3.global.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySseConnectionRepositoryTest {

    private final InMemorySseConnectionRepository repository = new InMemorySseConnectionRepository();

    @Test
    @DisplayName("사용자별로 여러 SseEmitter를 저장하고 조회할 수 있다")
    void saveAndFindEmittersByUserId() {
        UUID userId = UUID.randomUUID();
        SseEmitter firstEmitter = new SseEmitter();
        SseEmitter secondEmitter = new SseEmitter();

        String firstEmitterId = repository.saveEmitter(userId, firstEmitter);
        String secondEmitterId = repository.saveEmitter(userId, secondEmitter);

        assertThat(repository.findEmittersByUserId(userId))
                .containsEntry(firstEmitterId, firstEmitter)
                .containsEntry(secondEmitterId, secondEmitter);
    }

    @Test
    @DisplayName("SseEmitter를 삭제하고 사용자의 emitter가 없으면 빈 조회 결과를 반환한다")
    void deleteEmitter() {
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        String emitterId = repository.saveEmitter(userId, emitter);

        repository.deleteEmitter(userId, emitterId);

        assertThat(repository.findEmittersByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("사용자별 SseEmitter 저장소는 서로 격리된다")
    void emittersAreSeparatedByUserId() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        SseEmitter firstEmitter = new SseEmitter();
        SseEmitter secondEmitter = new SseEmitter();

        String firstEmitterId = repository.saveEmitter(firstUserId, firstEmitter);
        String secondEmitterId = repository.saveEmitter(secondUserId, secondEmitter);

        assertThat(repository.findEmittersByUserId(firstUserId))
                .containsEntry(firstEmitterId, firstEmitter)
                .doesNotContainEntry(secondEmitterId, secondEmitter);
        assertThat(repository.findEmittersByUserId(secondUserId))
                .containsEntry(secondEmitterId, secondEmitter)
                .doesNotContainEntry(firstEmitterId, firstEmitter);
    }

    @Test
    @DisplayName("사용자별 이벤트 캐시는 최신 100개만 유지한다")
    void eventCacheKeepsLatestOneHundredEvents() {
        UUID userId = UUID.randomUUID();

        IntStream.rangeClosed(1, 101)
                .mapToObj(index -> new SseEventCache(
                        "event-" + index,
                        "notifications",
                        "data-" + index,
                        Instant.parse("2026-06-24T00:00:00Z")
                ))
                .forEach(event -> repository.saveEvent(userId, event));

        assertThat(repository.findCachedEventsByUserId(userId))
                .hasSize(100)
                .extracting(SseEventCache::id)
                .doesNotContain("event-1")
                .containsExactlyElementsOf(
                        IntStream.rangeClosed(2, 101)
                                .mapToObj(index -> "event-" + index)
                                .toList()
                );
    }

    @Test
    @DisplayName("Last-Event-ID 이후의 이벤트 캐시를 조회할 수 있다")
    void findCachedEventsAfterLastEventId() {
        UUID userId = UUID.randomUUID();

        repository.saveEvent(userId, SseEventCache.of("event-1", "notifications", "data-1"));
        repository.saveEvent(userId, SseEventCache.of("event-2", "notifications", "data-2"));
        repository.saveEvent(userId, SseEventCache.of("event-3", "direct-messages", "data-3"));

        assertThat(repository.findCachedEventsAfter(userId, "event-1"))
                .extracting(SseEventCache::id)
                .containsExactly("event-2", "event-3");
    }

    @Test
    @DisplayName("Last-Event-ID가 없으면 현재 캐시 전체를 조회한다")
    void findCachedEventsAfterNullLastEventId() {
        UUID userId = UUID.randomUUID();

        repository.saveEvent(userId, SseEventCache.of("event-1", "notifications", "data-1"));
        repository.saveEvent(userId, SseEventCache.of("event-2", "notifications", "data-2"));

        assertThat(repository.findCachedEventsAfter(userId, null))
                .extracting(SseEventCache::id)
                .containsExactly("event-1", "event-2");
    }

    @Test
    @DisplayName("Last-Event-ID가 캐시에 없으면 현재 캐시 전체를 조회한다")
    void findCachedEventsAfterUnknownLastEventId() {
        UUID userId = UUID.randomUUID();

        repository.saveEvent(userId, SseEventCache.of("event-1", "notifications", "data-1"));
        repository.saveEvent(userId, SseEventCache.of("event-2", "notifications", "data-2"));

        assertThat(repository.findCachedEventsAfter(userId, "expired-event"))
                .extracting(SseEventCache::id)
                .containsExactly("event-1", "event-2");
    }

    @Test
    @DisplayName("사용자별 emitter와 이벤트 캐시를 전체 삭제할 수 있다")
    void deleteAllByUserId() {
        UUID userId = UUID.randomUUID();

        repository.saveEmitter(userId, new SseEmitter());
        repository.saveEvent(userId, SseEventCache.of("event-1", "notifications", "data-1"));

        repository.deleteAllEmitters(userId);
        repository.deleteAllCachedEvents(userId);

        assertThat(repository.findEmittersByUserId(userId)).isEmpty();
        assertThat(repository.findCachedEventsByUserId(userId)).isEmpty();
    }
}
