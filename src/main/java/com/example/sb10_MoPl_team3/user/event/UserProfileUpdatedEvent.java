package com.example.sb10_MoPl_team3.user.event;

import com.example.sb10_MoPl_team3.user.dto.response.UserSummary;

public record UserProfileUpdatedEvent(UserSummary user) {
}
