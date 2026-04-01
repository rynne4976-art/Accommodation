package com.Accommodation.repository;

import com.Accommodation.entity.Accom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccomRepository extends JpaRepository<Accom, Long>, AccomRepositoryCustom {
}