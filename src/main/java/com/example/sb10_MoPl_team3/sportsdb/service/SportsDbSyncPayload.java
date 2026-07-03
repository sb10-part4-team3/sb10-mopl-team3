package com.example.sb10_MoPl_team3.sportsdb.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import java.util.List;
import java.util.function.Supplier;

record SportsDbSyncPayload(
    String externalId,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tagNames,
    Supplier<Content> newContentSupplier
) {}