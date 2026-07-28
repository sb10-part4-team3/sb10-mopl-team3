package com.example.sb10_MoPl_team3.contentchat.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentChatSendRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("채팅 내용이 있으면 유효성 검증을 통과한다")
    void validate_success() {
        assertThat(validator.validate(new ContentChatSendRequest("안녕하세요"))).isEmpty();
    }

    @Test
    @DisplayName("채팅 내용이 null이거나 공백이면 유효성 검증에 실패한다")
    void validate_blankContent() {
        assertThat(validator.validate(new ContentChatSendRequest(null))).isNotEmpty();
        assertThat(validator.validate(new ContentChatSendRequest("   "))).isNotEmpty();
    }
}
