package com.example.sb10_MoPl_team3.content;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ContentType {
	@JsonProperty("movie") MOVIE,
	@JsonProperty("tvSeries") TV_SERIES,
	@JsonProperty("sport") SPORT;

	/**
	 * 쿼리 파라미터 등 Jackson이 개입하지 않는 경로에서 들어오는 문자열을
	 * ContentType으로 변환한다. camelCase 값과 enum 상수명(대소문자 무시)을 모두 허용한다.
	 */
	public static ContentType fromValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return switch (value.toLowerCase().replace("_", "")) {
			case "movie" -> MOVIE;
			case "tvseries" -> TV_SERIES;
			case "sport" -> SPORT;
			default -> throw new BusinessException(ErrorCode.INVALID_CONTENT_TYPE);
		};
	}
}