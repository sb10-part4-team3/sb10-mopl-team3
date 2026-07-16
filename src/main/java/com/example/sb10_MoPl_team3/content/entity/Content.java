package com.example.sb10_MoPl_team3.content.entity;

import com.example.sb10_MoPl_team3.content.ContentType;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import java.time.Instant;
import lombok.Builder;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "contents", uniqueConstraints = @UniqueConstraint(
    name = "uk_contents_external_id_source",
    columnNames = {"external_id", "source"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE contents SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Content extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private ContentType type;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "thumbnail_url")
  private String thumbnailUrl;

  @Column(name = "external_id", nullable = false)
  private String externalId;

  @Column(name = "source", nullable = false)
  private String source;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  // 정렬용 콘텐츠 기준 일시. 스포츠는 경기 일자, 영화/TV는 값이 없어 createdAt으로 대체된다.
  @Column(name = "event_date")
  private Instant eventDate;

  @Builder
  private Content(ContentType type, String title, String description, String thumbnailUrl,
      String externalId, String source, Instant eventDate) {

    if (type == null) {
      throw new IllegalArgumentException("type은 필수입니다");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title은 필수입니다");
    }
    if (externalId == null || externalId.isBlank()) {
      throw new IllegalArgumentException("externalId는 필수입니다");
    }
    if (source == null || source.isBlank()) {
      throw new IllegalArgumentException("source는 필수입니다");
    }

    this.type = type;
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
    this.externalId = externalId;
    this.source = source;
    this.eventDate = eventDate;
  }

  public void update(String title, String description) {
    if (title != null) {
      if (title.isBlank()) {
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
      }
      this.title = title;
    }
    if (description != null) {
      this.description = description;
    }
  }

  public void updateThumbnail(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public void syncFromExternal(String title, String description, String thumbnailUrl,
      Instant eventDate) {
    if (title == null || title.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
    this.eventDate = eventDate;
  }

}
