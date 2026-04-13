package com.Accommodation.repository;

import com.Accommodation.constant.AccomType;
import com.Accommodation.entity.Accom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccomRepository extends JpaRepository<Accom, Long>, AccomRepositoryCustom {

    @EntityGraph(attributePaths = {"operationPolicy", "operationDayList"})
    Optional<Accom> findWithOperationInfoById(Long id);

    @Query("""
            select distinct a.location
            from Accom a
            where a.deleted = false
              and a.location is not null
              and trim(a.location) <> ''
            order by a.location
            """)
    List<String> findDistinctActiveLocations();

    @Query("""
            select distinct a.accomType
            from Accom a
            where a.deleted = false
              and a.accomType is not null
            order by a.accomType
            """)
    List<AccomType> findDistinctActiveAccomTypes();
}
