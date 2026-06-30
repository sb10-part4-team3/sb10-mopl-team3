package com.example.sb10_MoPl_team3.watchingsession.repository;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import com.example.sb10_MoPl_team3.watchingsession.entity.WatchingSession;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class WatchingSessionRepositoryCustomImplTest {

    @Autowired
    private WatchingSessionRepository watchingSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("콘텐츠와 시청자 이름으로 필터링하고 createdAt 내림차순 커서를 적용한다")
    void findByContentDesc_filtersAndAppliesCursor() {
        Content content = saveContent("대상", "target");
        Content otherContent = saveContent("다른 콘텐츠", "other");
        User matched = saveUser("matched@test.com", "김시청자");
        User anotherMatched = saveUser("another@test.com", "이시청자");
        User thirdMatched = saveUser("third@test.com", "최시청자");
        User notMatched = saveUser("other@test.com", "다른 사람");
        User otherWatcher = saveUser("content@test.com", "박시청자");

        WatchingSession newest = saveSession(matched, content, "2026-06-30T03:00:00Z");
        WatchingSession cursorSession = saveSession(anotherMatched, content, "2026-06-30T02:00:00Z");
        WatchingSession oldest = saveSession(thirdMatched, content, "2026-06-30T01:00:00Z");
        saveSession(notMatched, content, "2026-06-30T00:30:00Z");
        saveSession(otherWatcher, otherContent, "2026-06-30T00:00:00Z");
        entityManager.flush();
        entityManager.clear();

        List<WatchingSession> firstPage = watchingSessionRepository.findByContentDesc(
                content.getId(), "시청자", null, null, PageRequest.of(0, 2));
        List<WatchingSession> secondPage = watchingSessionRepository.findByContentDesc(
                content.getId(), "시청자", cursorSession.getCreatedAt(), cursorSession.getId(),
                PageRequest.of(0, 2));

        assertThat(firstPage).extracting(WatchingSession::getId)
                .containsExactly(newest.getId(), cursorSession.getId());
        assertThat(secondPage).extracting(WatchingSession::getId)
                .containsExactly(oldest.getId());
        assertThat(watchingSessionRepository.countByContent(content.getId(), "시청자"))
                .isEqualTo(3L);
    }

    @Test
    @DisplayName("createdAt이 같은 세션은 idAfter를 타이브레이커로 사용해 다음 페이지로 이어진다")
    void findByContentDesc_usesIdAfterWhenCreatedAtIsSame() {
        Content content = saveContent("동일 시각 콘텐츠", "same-created-at");
        User firstWatcher = saveUser("tie-first@test.com", "첫 번째");
        User secondWatcher = saveUser("tie-second@test.com", "두 번째");
        User olderWatcher = saveUser("tie-older@test.com", "이전 시청자");
        Instant tiedAt = Instant.parse("2026-06-30T02:00:00Z");

        WatchingSession firstTied = saveSession(firstWatcher, content, tiedAt.toString());
        WatchingSession secondTied = saveSession(secondWatcher, content, tiedAt.toString());
        WatchingSession older = saveSession(olderWatcher, content, "2026-06-30T01:00:00Z");
        entityManager.flush();
        entityManager.clear();

        List<WatchingSession> firstPage = watchingSessionRepository.findByContentDesc(
                content.getId(), null, null, null, PageRequest.of(0, 1));
        WatchingSession pageCursor = firstPage.get(0);
        List<WatchingSession> secondPage = watchingSessionRepository.findByContentDesc(
                content.getId(), null, pageCursor.getCreatedAt(), pageCursor.getId(),
                PageRequest.of(0, 2));

        assertThat(firstPage).hasSize(1);
        assertThat(List.of(firstTied.getId(), secondTied.getId()))
                .contains(pageCursor.getId());
        assertThat(secondPage).extracting(WatchingSession::getId)
                .containsExactly(
                        pageCursor.getId().equals(firstTied.getId())
                                ? secondTied.getId()
                                : firstTied.getId(),
                        older.getId()
                );
        assertThat(secondPage).extracting(WatchingSession::getId)
                .doesNotContain(pageCursor.getId());
    }

    private User saveUser(String email, String name) {
        return userRepository.save(new User(email, name, "password", null, UserRole.USER));
    }

    private Content saveContent(String title, String externalId) {
        return contentRepository.save(Content.builder()
                .type(ContentType.MOVIE)
                .title(title)
                .description("설명")
                .thumbnailUrl("thumbnail")
                .externalId(externalId)
                .source("test")
                .build());
    }

    private WatchingSession saveSession(User watcher, Content content, String createdAt) {
        WatchingSession session = new WatchingSession(watcher, content);
        ReflectionTestUtils.setField(session, "createdAt", Instant.parse(createdAt));
        return watchingSessionRepository.save(session);
    }
}
