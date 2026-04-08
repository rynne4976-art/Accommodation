package com.Accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RegionActivityPageDto {

    private String regionName;
    private String heroImagePath;
    private String headline;
    private String subHeadline;
    private RegionFeaturedSectionDto featuredSection;
    private List<RegionActivityItemDto> items;
}
