package com.example.sb10_MoPl_team3.auth.password.event;

public record TemporaryPasswordIssuedEvent(
        String email,
        String temporaryPassword
) {
}
