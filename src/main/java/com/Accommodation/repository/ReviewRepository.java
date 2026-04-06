package com.Accommodation.repository;

import com.Accommodation.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"member", "reviewImgList"})
    List<Review> findByAccomIdOrderByRegTimeDesc(Long accomId);

    @EntityGraph(attributePaths = {"member", "reviewImgList"})
    Page<Review> findByAccomIdOrderByRegTimeDesc(Long accomId, Pageable pageable);

    long countByAccomId(Long accomId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.accom.id = :accomId")
    Double findAverageRatingByAccomId(@Param("accomId") Long accomId);

    boolean existsByMemberIdAndAccomId(Long memberId, Long accomId);

    @EntityGraph(attributePaths = {"member", "reviewImgList"})
    Optional<Review> findByMemberIdAndAccomId(Long memberId, Long accomId);
}
