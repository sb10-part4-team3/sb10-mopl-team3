package com.example.sb10_MoPl_team3.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.content.repository.ContentRepository;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.content.repository.TagRepository;
import com.example.sb10_MoPl_team3.global.config.JpaAuditingConfig;
import com.example.sb10_MoPl_team3.global.config.QuerydslConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 여러 스레드가 각자의 트랜잭션으로 같은 신규 태그명을 동시에 저장하는 상황을 재현한다.
 * {@code @DataJpaTest}가 테스트 메서드를 하나의 롤백 트랜잭션으로 감싸면 스레드들이 같은
 * 트랜잭션을 공유하게 되어 실제 동시성이 재현되지 않으므로, 클래스 단위로
 * {@code Propagation.NOT_SUPPORTED}를 걸어 각 스레드가 독립된 트랜잭션/커넥션을 쓰도록 한다.
 */
@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContentTagServiceConcurrencyTest {

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Autowired
  private TagRepository tagRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  // 클래스 전체가 NOT_SUPPORTED라 이 테스트가 만드는 데이터는 전부 실제로 커밋된다.
  // 다른 테스트 클래스와 H2 컨텍스트를 공유할 수 있으므로 명시적으로 정리한다.
  @AfterEach
  void cleanUp() {
    contentTagRepository.deleteAll();
    contentRepository.deleteAll();
    tagRepository.deleteAll();
  }

  @Test
  void 여러_콘텐츠가_동시에_같은_신규_태그명으로_저장해도_전부_성공하고_태그는_하나만_생성된다() {
    ContentTagServiceImpl contentTagService =
        new ContentTagServiceImpl(tagRepository, contentTagRepository, transactionManager);
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

    int threadCount = 8;
    List<Content> contents = java.util.stream.IntStream.range(0, threadCount)
        .mapToObj(i -> contentRepository.saveAndFlush(Content.builder()
            .type(ContentType.MOVIE)
            .title("동시성테스트콘텐츠" + i)
            .externalId("concurrency-ext-" + i)
            .source("MANUAL")
            .build()))
        .toList();

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    try {
      // 각 스레드가 독립된 트랜잭션에서 syncTags를 실행해야 실제 프로덕션 호출 경로
      // (transactionTemplate으로 감싸인 짧은 트랜잭션)와 동일한 조건으로 경합이 재현된다.
      List<CompletableFuture<Void>> futures = contents.stream()
          .map(content -> CompletableFuture.runAsync(
              () -> transactionTemplate.executeWithoutResult(
                  status -> contentTagService.syncTags(content, List.of("동시성신규태그"))),
              executor))
          .toList();

      // 하나라도 예외가 나면 join()에서 CompletionException으로 드러난다.
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    } finally {
      executor.shutdown();
    }

    // name 컬럼에 DB unique 제약(uk_tags_name)이 걸려 있어 중복 row 자체가 존재할 수 없다.
    // 여기서 검증하려는 건 그 제약을 위반한 스레드가 예외로 요청 전체를 날려버리지 않고
    // 정상적으로 기존 태그를 재사용해 넘어갔는지(= 위의 join()이 예외 없이 끝났는지)와,
    // 모든 콘텐츠가 실제로 그 태그에 연결됐는지다.
    assertThat(tagRepository.findByName("동시성신규태그")).isPresent();

    for (Content content : contents) {
      assertThat(contentTagRepository.findTagNamesByContentId(content.getId()))
          .containsExactly("동시성신규태그");
    }
  }
}
