package com.Accommodation.repository;

import com.Accommodation.entity.ActivityWish;
import com.Accommodation.entity.Activity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityWishRepository extends JpaRepository<ActivityWish, Long> {

    boolean existsByMemberEmailAndActivityKey(String email, String activityKey);

    Optional<ActivityWish> findByMemberEmailAndActivityKey(String email, String activityKey);

    List<ActivityWish> findByMemberEmailAndActivityKeyIn(String email, List<String> activityKeys);

    @EntityGraph(attributePaths = "activity")
    List<ActivityWish> findByMemberEmailOrderByRegTimeDesc(String email);

    long countByMemberEmail(String email);

    void deleteByActivityIn(List<Activity> activities);
}
