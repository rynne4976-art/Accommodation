package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransportRouteDto {

    private final double distanceKm;
    private final int durationMinutes;
    private final String source;
}
