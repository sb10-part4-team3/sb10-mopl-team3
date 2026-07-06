package com.example.sb10_MoPl_team3.auth.password.notification;

import com.example.sb10_MoPl_team3.auth.password.config.PasswordResetMailProperties;
import com.example.sb10_MoPl_team3.auth.password.exception.TemporaryPasswordSendFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev & !test")
@RequiredArgsConstructor
public class MailTemporaryPasswordNotifier implements TemporaryPasswordNotifier {

    private static final String SUBJECT = "[모두의 플리] 임시 비밀번호 안내";

    private final JavaMailSender mailSender;
    private final PasswordResetMailProperties properties;

    @Override
    public void send(String email, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(createBody(temporaryPassword));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new TemporaryPasswordSendFailedException(exception);
        }
    }

    private String createBody(String temporaryPassword) {
        return """
                안녕하세요. 모두의 플리입니다.

                임시 비밀번호가 발급되었습니다.

                임시 비밀번호: %s

                임시 비밀번호는 발급 후 3분 동안만 사용할 수 있습니다.
                임시 비밀번호는 1회 로그인에만 사용할 수 있습니다.
                로그인 후 새 비밀번호로 변경해 주세요.

                감사합니다.
                """.formatted(temporaryPassword);
    }
}