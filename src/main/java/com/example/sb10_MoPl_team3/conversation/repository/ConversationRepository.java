package com.example.sb10_MoPl_team3.conversation.repository;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  @EntityGraph(attributePaths = {"user1", "user2"})
  Optional<Conversation> findWithUsersById(UUID id);

  @Query("""
            select c
            from Conversation c
            join fetch c.user1 u1
            join fetch c.user2 u2
            where (u1.id = :userId or u2.id = :userId)
              and (
                :keyword is null
                or (u1.id = :userId and (
                  lower(u2.name) like lower(concat('%', :keyword, '%'))
                  or lower(u2.email) like lower(concat('%', :keyword, '%'))
                ))
                or (u2.id = :userId and (
                  lower(u1.name) like lower(concat('%', :keyword, '%'))
                  or lower(u1.email) like lower(concat('%', :keyword, '%'))
                ))
              )
              and (
                :cursor is null
                or c.createdAt > :cursor
                or (c.createdAt = :cursor and c.id > :idAfter)
              )
            order by c.createdAt asc, c.id asc
            """)
  List<Conversation> findParticipatingConversationsAsc(
      @Param("userId") UUID userId,
      @Param("keyword") String keyword,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  @Query("""
            select c
            from Conversation c
            join fetch c.user1 u1
            join fetch c.user2 u2
            where (u1.id = :userId or u2.id = :userId)
              and (
                :keyword is null
                or (u1.id = :userId and (
                  lower(u2.name) like lower(concat('%', :keyword, '%'))
                  or lower(u2.email) like lower(concat('%', :keyword, '%'))
                ))
                or (u2.id = :userId and (
                  lower(u1.name) like lower(concat('%', :keyword, '%'))
                  or lower(u1.email) like lower(concat('%', :keyword, '%'))
                ))
              )
              and (
                :cursor is null
                or c.createdAt < :cursor
                or (c.createdAt = :cursor and c.id < :idAfter)
              )
            order by c.createdAt desc, c.id desc
            """)
  List<Conversation> findParticipatingConversationsDesc(
      @Param("userId") UUID userId,
      @Param("keyword") String keyword,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  @Query("""
            select count(c)
            from Conversation c
            join c.user1 u1
            join c.user2 u2
            where (u1.id = :userId or u2.id = :userId)
              and (
                :keyword is null
                or (u1.id = :userId and (
                  lower(u2.name) like lower(concat('%', :keyword, '%'))
                  or lower(u2.email) like lower(concat('%', :keyword, '%'))
                ))
                or (u2.id = :userId and (
                  lower(u1.name) like lower(concat('%', :keyword, '%'))
                  or lower(u1.email) like lower(concat('%', :keyword, '%'))
                ))
              )
            """)
  long countParticipatingConversations(
      @Param("userId") UUID userId,
      @Param("keyword") String keyword
  );

  @EntityGraph(attributePaths = {"user1", "user2"})
  @Query("""
            select c
            from Conversation c
            where (c.user1.id = :userId1 and c.user2.id = :userId2)
               or (c.user1.id = :userId2 and c.user2.id = :userId1)
            """)
  Optional<Conversation> findByUserIds(
      @Param("userId1") UUID userId1,
      @Param("userId2") UUID userId2
  );
}
