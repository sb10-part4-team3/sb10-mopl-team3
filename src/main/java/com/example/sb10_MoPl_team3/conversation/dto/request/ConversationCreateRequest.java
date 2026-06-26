package com.example.sb10_MoPl_team3.conversation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
    @NotNull UUID withUserId
) {
}
