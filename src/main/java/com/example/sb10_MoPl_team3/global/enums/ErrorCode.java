package com.example.sb10_MoPl_team3.global.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "요청 파라미터 타입이 올바르지 않습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "커서 값이 올바르지 않습니다."),
    INVALID_SORT_DIRECTION(HttpStatus.BAD_REQUEST, "정렬값이 올바르지 않습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 리뷰
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),


    // 콘텐츠
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."),

    // 플레이리스트
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트를 찾을 수 없습니다.");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
