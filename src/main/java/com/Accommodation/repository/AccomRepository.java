package com.Accommodation.repository;
import com.Accommodation.entity.Accom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccomRepository
        extends JpaRepository<Accom, Long>, QuerydslPredicateExecutor<Accom>
        , AccomRepositoryCustom {
    List<Accom> findByAccomNm(String accomNm);

    List<Accom> findByAccomNmOrAccomDetail(String accomNm, String accomDetail);

    List<Accom> findByPriceLessThan(Integer price);

    List<Accom> findByPriceLessThanOrderByPriceDesc(Integer price);

    @Query("select a from Accom a where a.accomDetail like concat('%', :accomDetail, '%') " +
            "order by a.price desc")
    List<Accom> findByAccomDetail(@Param("accomDetail") String accomDetail);

    @Query(value = "select * from accom a where a.accom_detail like concat('%', :accomDetail, '%') " +
            "order by a.price desc", nativeQuery = true)
    List<Accom> findByAccomDetailByNative(@Param("accomDetail") String accomDetail);
}
