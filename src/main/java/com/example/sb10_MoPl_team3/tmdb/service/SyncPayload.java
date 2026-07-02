package com.example.sb10_MoPl_team3.tmdb.service;

import com.example.sb10_MoPl_team3.content.entity.Content;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

record SyncPayload(
    String externalId,
    String title,
    String overview,
    String posterPath,
    List<String> genreNames,
    Supplier<Content> newContentSupplier
) {}