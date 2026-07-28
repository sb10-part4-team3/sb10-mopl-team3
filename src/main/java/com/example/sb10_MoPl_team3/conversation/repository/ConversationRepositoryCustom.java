package com.example.sb10_MoPl_team3.conversation.repository;

import com.example.sb10_MoPl_team3.conversation.entity.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ConversationRepositoryCustom {

  List<Conversation> findParticipatingConversationsAsc(
      UUID userId,
      String keyword,
      Instant cursor,
      UUID idAfter,
      Pageable pageable
  );

  List<Conversation> findParticipatingConversationsDesc(
      UUID userId,
      String keyword,
      Instant cursor,
      UUID idAfter,
      Pageable pageable
  );

  long countParticipatingConversations(UUID userId, String keyword);
}
