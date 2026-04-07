package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransportGeocodeDto {

    private final double lat;
    private final double lng;
    private final String address;
}
