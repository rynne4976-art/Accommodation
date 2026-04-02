package com.Accommodation.repository;

import com.Accommodation.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"member", "reviewImgList"})
    List<Review> findByAccomIdOrderByRegTimeDesc(Long accomId);

    boolean existsByMemberIdAndAccomId(Long memberId, Long accomId);

    @EntityGraph(attributePaths = {"member", "reviewImgList"})
    Optional<Review> findByMemberIdAndAccomId(Long memberId, Long accomId);
}
