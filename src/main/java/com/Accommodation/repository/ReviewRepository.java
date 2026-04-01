package com.Accommodation.repository;

import com.Accommodation.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByMemberIdAndAccomId(Long memberId, Long accomId);
    List<Review> findByAccomIdOrderByRegTimeDesc(Long accomId);
}
