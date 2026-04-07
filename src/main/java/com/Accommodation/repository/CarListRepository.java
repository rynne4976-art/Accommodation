package com.Accommodation.repository;

import com.Accommodation.entity.CarList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarListRepository extends JpaRepository<CarList, Long> {

    List<CarList> findTop3ByOrderByCarPriceAsc();

    List<CarList> findAllByOrderByCarPriceAsc();
}
