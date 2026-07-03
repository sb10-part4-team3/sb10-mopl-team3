package com.example.sb10_MoPl_team3.notification.event;

@FunctionalInterface
public interface NotificationEventHandler {

    void handle(NotificationEvent event);
}
