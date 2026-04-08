package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RegionFeaturedSectionDto {

    private String title;
    private String subtitle;
    private List<RegionFeaturedCardDto> cards;
}
