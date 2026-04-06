package com.Accommodation.repository;

import com.Accommodation.entity.AccomImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccomImgRepository extends JpaRepository<AccomImg, Long> {

    List<AccomImg> findByAccomIdOrderByIdAsc(Long accomId);

    @Modifying
    @Query("delete from AccomImg ai where ai.accom.id = :accomId")
    void deleteByAccomId(@Param("accomId") Long accomId);
}
