package com.example.sb10_MoPl_team3.directmessage.repository;

import com.example.sb10_MoPl_team3.directmessage.entity.DirectMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface DirectMessageRepositoryCustom {

    List<DirectMessage> findByConversationIdWithCursorAsc(
        UUID conversationId,
        Instant cursor,
        UUID idAfter,
        Pageable pageable
    );

    List<DirectMessage> findByConversationIdWithCursorDesc(
        UUID conversationId,
        Instant cursor,
        UUID idAfter,
        Pageable pageable
    );
}
