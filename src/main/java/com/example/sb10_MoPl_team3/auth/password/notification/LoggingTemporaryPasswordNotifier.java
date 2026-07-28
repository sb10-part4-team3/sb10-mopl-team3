package com.example.sb10_MoPl_team3.auth.password.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "auth.password-reset-mail",
        name = "mode",
        havingValue = "log",
        matchIfMissing = true
)
public class LoggingTemporaryPasswordNotifier implements TemporaryPasswordNotifier {

    @Override
    public void send(String email, String temporaryPassword) {
        log.info("Temporary password issued. email={}", email);
    }
}
