package com.example.sb10_MoPl_team3.review.repository;

import com.example.sb10_MoPl_team3.review.entity.Review;
import com.example.sb10_MoPl_team3.review.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

    long countByContent_IdAndStatus(UUID contentId, ReviewStatus status);
}
