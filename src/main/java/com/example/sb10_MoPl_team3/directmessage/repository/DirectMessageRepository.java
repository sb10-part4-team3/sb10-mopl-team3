package com.example.sb10_MoPl_team3.directmessage.repository;

import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    @EntityGraph(attributePaths = {"conversation", "sender", "receiver"})
    Slice<DirectMessage> findByConversationId(UUID conversationId, Pageable pageable);

    @Query("""
            select dm
            from DirectMessage dm
            join fetch dm.conversation c
            join fetch dm.sender
            join fetch dm.receiver
            where c.id = :conversationId
              and (
                :cursor is null
                or dm.createdAt > :cursor
                or (dm.createdAt = :cursor and dm.id > :idAfter)
              )
            order by dm.createdAt asc, dm.id asc
            """)
    List<DirectMessage> findByConversationIdWithCursorAsc(
        @Param("conversationId") UUID conversationId,
        @Param("cursor") Instant cursor,
        @Param("idAfter") UUID idAfter,
        Pageable pageable
    );

    @Query("""
            select dm
            from DirectMessage dm
            join fetch dm.conversation c
            join fetch dm.sender
            join fetch dm.receiver
            where c.id = :conversationId
              and (
                :cursor is null
                or dm.createdAt < :cursor
                or (dm.createdAt = :cursor and dm.id < :idAfter)
              )
            order by dm.createdAt desc, dm.id desc
            """)
    List<DirectMessage> findByConversationIdWithCursorDesc(
        @Param("conversationId") UUID conversationId,
        @Param("cursor") Instant cursor,
        @Param("idAfter") UUID idAfter,
        Pageable pageable
    );

    long countByConversationId(UUID conversationId);

    @EntityGraph(attributePaths = {"conversation", "sender", "receiver"})
    Optional<DirectMessage> findByIdAndConversationId(UUID id, UUID conversationId);
}
