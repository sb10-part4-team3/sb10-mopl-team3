package com.example.sb10_MoPl_team3.directmessage.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class DirectMessageSendRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("쪽지 내용은 null이거나 공백일 수 없다")
    void contentMustNotBeBlank() {
        assertThat(validator.validate(new DirectMessageSendRequest("메시지"))).isEmpty();
        assertThat(validator.validate(new DirectMessageSendRequest(null))).isNotEmpty();
        assertThat(validator.validate(new DirectMessageSendRequest("   "))).isNotEmpty();
    }
}
