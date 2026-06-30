package com.example.sb10_MoPl_team3.contentchat.service;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.user.entity.User;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import com.example.sb10_MoPl_team3.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContentChatServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContentChatService contentChatService;

    @Test
    @DisplayName("발신자 정보가 포함된 휘발성 채팅 메시지를 생성한다")
    void createMessage_success() {
        UUID contentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        User sender = new User("sender@test.com", "시청자", "password", null, UserRole.USER);
        ReflectionTestUtils.setField(sender, "id", senderId);
        given(userRepository.findById(senderId)).willReturn(Optional.of(sender));

        var result = contentChatService.createMessage(contentId, senderId, "같이 봐요");

        assertThat(result.id()).isNotNull();
        assertThat(result.contentId()).isEqualTo(contentId);
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.sender().userId()).isEqualTo(senderId);
        assertThat(result.sender().name()).isEqualTo("시청자");
        assertThat(result.content()).isEqualTo("같이 봐요");
    }

    @Test
    @DisplayName("발신자를 찾을 수 없으면 USER_NOT_FOUND 예외를 던진다")
    void createMessage_userNotFound() {
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(userRepository.findById(senderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contentChatService.createMessage(
                UUID.randomUUID(), senderId, "메시지"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
