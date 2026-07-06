package com.example.sb10_MoPl_team3.auth.password.notification;

import com.example.sb10_MoPl_team3.auth.password.config.PasswordResetMailProperties;
import com.example.sb10_MoPl_team3.auth.password.exception.TemporaryPasswordSendFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class MailTemporaryPasswordNotifierTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("임시 비밀번호 안내 메일을 발송한다")
    void send_success() {
        PasswordResetMailProperties properties =
                new PasswordResetMailProperties("no-reply@mopl.test");
        MailTemporaryPasswordNotifier notifier =
                new MailTemporaryPasswordNotifier(mailSender, properties);

        notifier.send("user@test.com", "tempPassword1!");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        then(mailSender).should().send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getFrom()).isEqualTo("no-reply@mopl.test");
        assertThat(message.getTo()).containsExactly("user@test.com");
        assertThat(message.getSubject()).isEqualTo("[모두의 플리] 임시 비밀번호 안내");
        assertThat(message.getText())
                .contains("tempPassword1!");
    }

    @Test
    @DisplayName("메일 발송에 실패하면 임시 비밀번호 발송 실패 예외가 발생한다")
    void send_failure() {
        PasswordResetMailProperties properties =
                new PasswordResetMailProperties("no-reply@mopl.test");
        MailTemporaryPasswordNotifier notifier =
                new MailTemporaryPasswordNotifier(mailSender, properties);

        doThrow(new MailSendException("smtp failed"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> notifier.send("user@test.com", "tempPassword1!"))
                .isInstanceOf(TemporaryPasswordSendFailedException.class);
    }
}
