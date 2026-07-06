package com.example.sb10_MoPl_team3.content.repository;

import java.util.UUID;

public interface ContentStatsRepositoryCustom {

  /**
   * 콘텐츠에 대한 기본 통계를 새로 만든다. 서버 인스턴스가 여러 대라 같은 콘텐츠에 대해
   * 동시에 호출되면, 이미 만들어진 쪽을 제외한 나머지는 PK(content_id) 중복으로 실패한다.
   * 별도 트랜잭션에서 실행되므로 실패해도 이 트랜잭션만 롤백되고 호출자의 트랜잭션은 영향받지 않는다.
   */
  void createDefaultIgnoringConflict(UUID contentId);
}
