package com.Accommodation.repository;

import com.Accommodation.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Optional<Activity> findByActivityKey(String activityKey);

    List<Activity> findByRegionNameAndExpiresAtAfterOrderBySortOrderAscTitleAsc(String regionName, LocalDateTime now);

    List<Activity> findByRegionNameOrderBySortOrderAscTitleAsc(String regionName);
}
