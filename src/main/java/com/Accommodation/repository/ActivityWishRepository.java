package com.Accommodation.repository;

import com.Accommodation.entity.ActivityWish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityWishRepository extends JpaRepository<ActivityWish, Long> {

    boolean existsByMemberEmailAndActivityKey(String email, String activityKey);

    Optional<ActivityWish> findByMemberEmailAndActivityKey(String email, String activityKey);

    List<ActivityWish> findByMemberEmailAndActivityKeyIn(String email, List<String> activityKeys);

    List<ActivityWish> findByMemberEmailOrderByRegTimeDesc(String email);

    long countByMemberEmail(String email);
}
