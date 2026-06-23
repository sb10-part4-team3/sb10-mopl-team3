package com.example.sb10_MoPl_team3.domain.review.entity;


import com.example.sb10_MoPl_team3.global.base.BaseEntity;
import com.example.sb10_MoPl_team3.domain.review.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "reviews")
public class Review extends BaseEntity {

    // 콘텐츠
    @Column(nullable = false)
    private Content content;

    // 리뷰 내용
    @Column(nullable = false, length = 500)
    private String text;

    // 평점
    @Column(nullable = false)
    private Double rating;

    // 리뷰 상태 - 논리 삭제 구분 용
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    // 삭제일시
    private Instant deletedAt;

    // 생성자
    @Builder
    public void Review(Content content, String text, Double rating, ReviewStatus status) {
        this.content = content;
        this.text = text;
        this.rating = rating;
        this.status = status;
    }

    // 리뷰 수정
    public void update(String text, Double rating) {
        if (text != null)
            this.text = text;
        if (rating != null)
            this.rating = rating;
    }

}
