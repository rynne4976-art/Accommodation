package com.Accommodation.dto;

import java.util.List;

public record OrderHistPage(
        List<OrderHistDto> orders,
        int totalCount,
        int totalPages,
        int currentPage
) {}
