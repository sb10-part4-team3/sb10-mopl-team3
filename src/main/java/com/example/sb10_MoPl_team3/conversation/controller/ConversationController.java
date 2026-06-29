package com.example.sb10_MoPl_team3.conversation.controller;

import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationCreateRequest;
import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationFindAllRequest;
import com.example.sb10_MoPl_team3.conversation.dto.response.CursorResponseConversationDto;
import com.example.sb10_MoPl_team3.conversation.dto.response.ConversationDto;
import com.example.sb10_MoPl_team3.conversation.service.ConversationService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<CursorResponseConversationDto<ConversationDto>> findConversations(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestParam(required = false) String keywordLike,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit,
        @RequestParam String sortDirection,
        @RequestParam String sortBy
    ) {
        ConversationFindAllRequest request = new ConversationFindAllRequest(
            keywordLike,
            cursor,
            idAfter,
            limit,
            sortDirection,
            sortBy
        );
        CursorResponseConversationDto<ConversationDto> response = conversationService.findAll(
            authUser.userId(),
            request
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> findConversation(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable UUID conversationId
    ) {
        ConversationDto response = conversationService.find(authUser.userId(), conversationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/with")
    public ResponseEntity<ConversationDto> findConversationWithUser(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestParam UUID userId
    ) {
        ConversationDto response = conversationService.findWithUser(authUser.userId(), userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody ConversationCreateRequest request
    ) {
        ConversationDto response = conversationService.create(authUser.userId(), request);
        return ResponseEntity.ok(response);
    }
}
