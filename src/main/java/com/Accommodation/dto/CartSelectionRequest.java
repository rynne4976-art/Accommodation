package com.Accommodation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartSelectionRequest {

    private List<Long> cartItemIds;
}
