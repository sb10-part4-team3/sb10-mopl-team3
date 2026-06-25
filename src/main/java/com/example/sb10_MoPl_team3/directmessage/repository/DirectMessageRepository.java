package com.example.sb10_MoPl_team3.directmessage.repository;

import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    @EntityGraph(attributePaths = {"conversation", "sender", "receiver"})
    List<DirectMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    @EntityGraph(attributePaths = {"conversation", "sender", "receiver"})
    Optional<DirectMessage> findByIdAndConversationId(UUID id, UUID conversationId);
}
