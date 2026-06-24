package com.example.sb10_MoPl_team3.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name="content_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ContentStats {

  @Id
  @Column(name = "content_id")
  private UUID id;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id")
  private Content content;

  @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal averageRating = BigDecimal.ZERO;

  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0;

  @Column(name = "viewer_count", nullable = false)
  private int viewerCount = 0;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Builder
  private ContentStats(Content content,BigDecimal averageRating, int reviewCount, int viewerCount){
    this.content = content;
    this.averageRating = averageRating;
    this.reviewCount = reviewCount;
    this.viewerCount = viewerCount;
  }
}
