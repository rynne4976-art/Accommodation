package com.Accommodation.controller;

import com.Accommodation.dto.RegionActivityPageDto;
import com.Accommodation.service.RegionActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class RegionActivityController {

    private final RegionActivityService regionActivityService;

    @GetMapping("/activities/{region}")
    public String regionActivities(@PathVariable("region") String region, Model model) {
        RegionActivityPageDto pageDto = regionActivityService.getRegionActivityPage(region);

        model.addAttribute("activityPage", pageDto);
        model.addAttribute("activityList", pageDto.getItems());

        return "category/activityList";
    }
}