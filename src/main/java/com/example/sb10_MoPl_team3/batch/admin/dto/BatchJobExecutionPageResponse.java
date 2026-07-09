package com.example.sb10_MoPl_team3.batch.admin.dto;

import java.util.List;

public record BatchJobExecutionPageResponse<T>(
    List<T> content,
    int page,
    int size,
    boolean hasNext,
    long totalCount
) {
}
