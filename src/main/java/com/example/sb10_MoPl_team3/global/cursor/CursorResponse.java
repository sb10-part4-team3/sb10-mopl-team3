package com.example.sb10_MoPl_team3.global.cursor;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record CursorResponse<T>(
    List<T> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

    /**
     * 리포지토리에서 size + 1 건을 조회했다는 가정 하에, 다음 페이지 존재 여부를 판단하고 잘라낸다.
     */
    public static <T> CursorResponse<T> of(
        List<T> fetched,
        int requestSize,
        long totalCount,
        String sortBy,
        String sortDirection,
        Function<T, String> cursorExtractor,
        Function<T, UUID> idExtractor
    ) { if(requestSize <= 0){
        throw new IllegalArgumentException("requestSize는 0보다 같거나 작을 수 없습니다.");
    }


        boolean hasNext = fetched.size() > requestSize;
        List<T> data = hasNext ? fetched.subList(0, requestSize) : fetched;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext) {
            T last = data.get(data.size() - 1);
            nextCursor = cursorExtractor.apply(last);
            nextIdAfter = idExtractor.apply(last);
        }

        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
    }

    public static <T> CursorResponse<T> empty(String sortBy, String sortDirection) {
        return new CursorResponse<>(List.of(), null, null, false, 0L, sortBy, sortDirection);
    }
}
