package com.Accommodation.controller;

import com.Accommodation.dto.RegionActivityPageDto;
import com.Accommodation.dto.RegionActivityItemDto;
import com.Accommodation.service.RegionActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RegionActivityController {

    private final RegionActivityService regionActivityService;
    private static final int PAGE_SIZE = 5;

    @GetMapping("/activities/{region}")
    public String regionActivities(@PathVariable("region") String region,
                                   @RequestParam(name = "page", defaultValue = "1") int page,
                                   Model model) {
        RegionActivityPageDto pageDto = regionActivityService.getRegionActivityPage(region);
        List<RegionActivityItemDto> allItems = pageDto.getItems();

        int totalItems = allItems == null ? 0 : allItems.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / PAGE_SIZE);
        int currentPage = totalPages == 0 ? 1 : Math.max(1, Math.min(page, totalPages));
        int startIndex = totalItems == 0 ? 0 : (currentPage - 1) * PAGE_SIZE;
        int endIndex = totalItems == 0 ? 0 : Math.min(startIndex + PAGE_SIZE, totalItems);
        List<RegionActivityItemDto> pagedItems = totalItems == 0 ? List.of() : allItems.subList(startIndex, endIndex);

        model.addAttribute("activityPage", pageDto);
        model.addAttribute("activityList", pagedItems);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPath", "/activities/" + region);

        return "category/activityList";
    }
}
