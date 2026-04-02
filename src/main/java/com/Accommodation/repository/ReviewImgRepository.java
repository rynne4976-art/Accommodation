package com.Accommodation.repository;

import com.Accommodation.entity.ReviewImg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewImgRepository extends JpaRepository<ReviewImg, Long> {

    List<ReviewImg> findByReviewId(Long reviewId);

    Optional<ReviewImg> findByIdAndReviewId(Long reviewImgId, Long reviewId);
}