package com.example.sb10_MoPl_team3.auth.password.notification;

public interface TemporaryPasswordNotifier {

    void send(String email, String temporaryPassword);
}