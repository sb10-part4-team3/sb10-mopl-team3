package com.example.sb10_MoPl_team3.conversation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class ConversationRepositoryCustomImplTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("참여 중인 대화방만 createdAt 오름차순으로 조회한다")
    void findParticipatingConversationsAsc_onlyParticipant() {
        User me = saveUser("me@test.com", "나");
        User userA = saveUser("a@test.com", "상대A");
        User userB = saveUser("b@test.com", "상대B");
        User outsider = saveUser("outsider@test.com", "외부인");

        Conversation older = saveConversation(me, userA, Instant.parse("2026-06-29T00:00:00Z"));
        Conversation newer = saveConversation(me, userB, Instant.parse("2026-06-29T00:01:00Z"));
        saveConversation(userA, outsider, Instant.parse("2026-06-29T00:02:00Z"));

        em.clear();

        List<Conversation> result = conversationRepository.findParticipatingConversationsAsc(
            me.getId(),
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );

        assertThat(result)
            .extracting(Conversation::getId)
            .containsExactly(older.getId(), newer.getId());
    }

    @Test
    @DisplayName("키워드는 요청자 본인이 아닌 상대 사용자 이름과 이메일에 적용된다")
    void findParticipatingConversations_keywordSearchesOtherUser() {
        User me = saveUser("owner@test.com", "본인");
        User matchedByName = saveUser("name@test.com", "target상대");
        User matchedByEmail = saveUser("matched-target@test.com", "이메일상대");
        User notMatched = saveUser("other@test.com", "다른상대");

        Conversation first = saveConversation(
            me,
            matchedByName,
            Instant.parse("2026-06-29T00:00:00Z")
        );
        Conversation second = saveConversation(
            me,
            matchedByEmail,
            Instant.parse("2026-06-29T00:01:00Z")
        );
        saveConversation(me, notMatched, Instant.parse("2026-06-29T00:02:00Z"));

        em.clear();

        List<Conversation> result = conversationRepository.findParticipatingConversationsAsc(
            me.getId(),
            "target",
            null,
            null,
            PageRequest.of(0, 10)
        );
        long count = conversationRepository.countParticipatingConversations(me.getId(), "target");

        assertThat(result)
            .extracting(Conversation::getId)
            .containsExactly(first.getId(), second.getId());
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("오름차순 커서 이후 대화방만 조회한다")
    void findParticipatingConversationsAsc_cursor() {
        User me = saveUser("me@test.com", "나");
        User userA = saveUser("a@test.com", "상대A");
        User userB = saveUser("b@test.com", "상대B");
        User userC = saveUser("c@test.com", "상대C");

        saveConversation(me, userA, Instant.parse("2026-06-29T00:00:00Z"));
        Conversation cursor = saveConversation(me, userB, Instant.parse("2026-06-29T00:01:00Z"));
        Conversation after = saveConversation(me, userC, Instant.parse("2026-06-29T00:02:00Z"));

        em.clear();

        List<Conversation> result = conversationRepository.findParticipatingConversationsAsc(
            me.getId(),
            null,
            cursor.getCreatedAt(),
            cursor.getId(),
            PageRequest.of(0, 10)
        );

        assertThat(result)
            .extracting(Conversation::getId)
            .containsExactly(after.getId());
    }

    @Test
    @DisplayName("내림차순 커서 이후 대화방만 조회한다")
    void findParticipatingConversationsDesc_cursor() {
        User me = saveUser("me@test.com", "나");
        User userA = saveUser("a@test.com", "상대A");
        User userB = saveUser("b@test.com", "상대B");
        User userC = saveUser("c@test.com", "상대C");

        Conversation oldest = saveConversation(me, userA, Instant.parse("2026-06-29T00:00:00Z"));
        Conversation cursor = saveConversation(me, userB, Instant.parse("2026-06-29T00:01:00Z"));
        saveConversation(me, userC, Instant.parse("2026-06-29T00:02:00Z"));

        em.clear();

        List<Conversation> result = conversationRepository.findParticipatingConversationsDesc(
            me.getId(),
            null,
            cursor.getCreatedAt(),
            cursor.getId(),
            PageRequest.of(0, 10)
        );

        assertThat(result)
            .extracting(Conversation::getId)
            .containsExactly(oldest.getId());
    }

    private User saveUser(String email, String name) {
        User user = new User(email, name, "password", null, UserRole.USER);
        return userRepository.saveAndFlush(user);
    }

    private Conversation saveConversation(User user1, User user2, Instant createdAt) {
        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(user1, user2));
        updateCreatedAt(conversation.getId(), createdAt);
        ReflectionTestUtils.setField(conversation, "createdAt", createdAt);
        em.flush();
        return conversation;
    }

    private void updateCreatedAt(UUID id, Instant createdAt) {
        em.createQuery("update Conversation c set c.createdAt = :createdAt where c.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", id)
            .executeUpdate();
    }
}
