package com.example.sb10_MoPl_team3.conversation.repository;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

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
