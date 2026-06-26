package com.example.sb10_MoPl_team3.conversation.controller;

import com.example.sb10_MoPl_team3.conversation.dto.request.ConversationCreateRequest;
import com.example.sb10_MoPl_team3.conversation.dto.response.ConversationDto;
import com.example.sb10_MoPl_team3.conversation.service.ConversationService;
import com.example.sb10_MoPl_team3.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody ConversationCreateRequest request
    ) {
        ConversationDto response = conversationService.create(authUser.userId(), request);
        return ResponseEntity.ok(response);
    }
}
