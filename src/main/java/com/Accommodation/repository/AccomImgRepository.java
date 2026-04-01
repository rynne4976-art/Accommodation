package com.Accommodation.repository;

import com.Accommodation.entity.AccomImg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccomImgRepository extends JpaRepository<AccomImg, Long> {

    List<AccomImg> findByAccomIdOrderByIdAsc(Long accomId);
}