package com.Accommodation.repository;

import com.Accommodation.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByMemberEmailAndAccomId(String email, Long accomId);

    Optional<Wish> findByMemberEmailAndAccomId(String email, Long accomId);

    List<Wish> findByMemberEmailOrderByRegTimeDesc(String email);
}
