package com.example.sb10_MoPl_team3.global.cursor;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import java.util.UUID;
import java.util.function.Function;

/**
 * 모든 도메인의 커서를 (정렬 기준 값, ID) 쌍으로 통일하기 위한 공통 구조.
 * 정렬값의 실제 타입(Instant, Long 등)에 의존하지 않도록 파싱 함수를 외부에서 주입받는다.
 */
public record Cursor<T>(T sortValue, UUID id) {

    public static <T> Cursor<T> from(CursorPageRequest request, Function<String, T> sortValueParser) {
        if (!request.hasCursor()) {
            return null;
        }
        try {
            return new Cursor<>(sortValueParser.apply(request.cursor()), request.idAfter());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }
}
