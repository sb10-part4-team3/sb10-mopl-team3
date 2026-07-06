package com.example.sb10_MoPl_team3.auth.password.notification;

import com.example.sb10_MoPl_team3.auth.password.event.TemporaryPasswordIssuedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TemporaryPasswordMailEventListener {

    private final TemporaryPasswordNotifier temporaryPasswordNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TemporaryPasswordIssuedEvent event) {
        temporaryPasswordNotifier.send(event.email(), event.temporaryPassword());
    }
}
