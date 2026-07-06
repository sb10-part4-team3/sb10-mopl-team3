package com.example.sb10_MoPl_team3.auth.password.notification;

import com.example.sb10_MoPl_team3.auth.password.event.TemporaryPasswordIssuedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordMailEventListenerTest {

    @Mock
    private TemporaryPasswordNotifier temporaryPasswordNotifier;

    @Test
    @DisplayName("임시 비밀번호 발급 이벤트를 받으면 메일 발송을 요청한다")
    void handle_success() {
        TemporaryPasswordMailEventListener listener =
                new TemporaryPasswordMailEventListener(temporaryPasswordNotifier);
        TemporaryPasswordIssuedEvent event =
                new TemporaryPasswordIssuedEvent("user@test.com", "tempPassword1!");

        listener.handle(event);

        then(temporaryPasswordNotifier).should()
                .send("user@test.com", "tempPassword1!");
    }
}
