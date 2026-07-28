package com.example.sb10_MoPl_team3.tmdb;

public final class TmdbConstants {

  public static final String SOURCE_TMDB = "TMDB";
  // TMDB API는 page > 500 요청 시 total_pages 값과 무관하게 400을 반환한다.
  public static final int MAX_PAGE = 500;
  private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

  private TmdbConstants() {
  }

  public static String toFullImageUrl(String posterPath) {
    if (posterPath == null || posterPath.isBlank()) {
      return null;
    }
    return IMAGE_BASE_URL + posterPath;
  }

  public static String externalId(String prefix, long id) {
    return prefix + "-" + id;
  }
}