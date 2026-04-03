package com.Accommodation.repository;

import com.Accommodation.dto.OrderSearchDto;
import com.Accommodation.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

    Page<Order> searchOrders(OrderSearchDto orderSearchDto, Pageable pageable);
}
