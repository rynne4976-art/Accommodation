package com.Accommodation.repository;

import com.Accommodation.entity.Accom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccomRepository extends JpaRepository<Accom, Long>, AccomRepositoryCustom {

    @EntityGraph(attributePaths = "accomImgList")
    Optional<Accom> findWithAccomImgListById(Long id);
}
