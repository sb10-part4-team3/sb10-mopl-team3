package com.example.sb10_MoPl_team3.content.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class ContentTag {

  @EmbeddedId
  private ContentTagId id;

  @MapsId("contentId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id")
  private Content content;

  @MapsId("tagId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id")
  private Tag tag;

  public ContentTag(Content content, Tag tag) {

    this.content = Objects.requireNonNull(content, "content must not be null");
    this.tag = Objects.requireNonNull(tag, "tag must not be null");
    if (content.getId() == null || tag.getId() == null) {
      throw new IllegalArgumentException(
          "content/tag must be persisted before ContentTag creation");
    }
    this.id = new ContentTagId(content.getId(), tag.getId());
  }
}
