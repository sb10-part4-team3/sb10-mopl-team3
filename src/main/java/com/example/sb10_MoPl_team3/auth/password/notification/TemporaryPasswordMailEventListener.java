package com.example.sb10_MoPl_team3.auth.password.notification;

import com.example.sb10_MoPl_team3.auth.password.event.TemporaryPasswordIssuedEvent;
import com.example.sb10_MoPl_team3.auth.password.exception.TemporaryPasswordSendFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryPasswordMailEventListener {

    private final TemporaryPasswordNotifier temporaryPasswordNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TemporaryPasswordIssuedEvent event) {
        try {
            temporaryPasswordNotifier.send(event.email(), event.temporaryPassword());
        } catch (TemporaryPasswordSendFailedException exception) {
            log.warn("Failed to send temporary password mail.", exception);
        }
    }
}