package com.Accommodation.dto;

import com.Accommodation.constant.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSearchDto {
    private String searchBy;
    private String searchQuery;
    private OrderStatus orderStatus;
}
