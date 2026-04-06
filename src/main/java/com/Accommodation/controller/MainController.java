package com.Accommodation.controller;

import com.Accommodation.constant.AccomType;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.service.AccomService;
import org.springframework.web.bind.annotation.CookieValue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final AccomService accomService;

    @GetMapping({"/", "/main"})
    public String main(AccomSearchDto accomSearchDto,
                       @RequestParam(value = "page") Optional<Integer> page,
                       Model model) {
        Pageable pageable = PageRequest.of(page.orElse(0), 10);
        Page<MainAccomDto> accomPage = accomService.getMainAccomPage(accomSearchDto, pageable);

        model.addAttribute("accomList", accomPage.getContent());
        model.addAttribute("accomPage", accomPage);
        model.addAttribute("accomSearchDto", accomSearchDto);
        model.addAttribute("maxPage", 5);

        return "main";
    }

    @GetMapping("/main/hotels")
    public String hotels(AccomSearchDto accomSearchDto,
                         @RequestParam(value = "page") Optional<Integer> page,
                         Model model) {
        return renderTypePage(
                "category/hotel",
                "/main/hotels",
                AccomType.HOTEL,
                "호텔",
                "도심과 여행지의 감도를 담은 호텔 숙소를 모았습니다.",
                accomSearchDto,
                page,
                model
        );
    }

    @GetMapping("/main/resorts")
    public String resorts(AccomSearchDto accomSearchDto,
                          @RequestParam(value = "page") Optional<Integer> page,
                          Model model) {
        return renderTypePage(
                "category/resort",
                "/main/resorts",
                AccomType.RESORT,
                "리조트",
                "휴양과 레저를 함께 즐길 수 있는 리조트를 확인하세요.",
                accomSearchDto,
                page,
                model
        );
    }

    @GetMapping("/main/pensions")
    public String pensions(AccomSearchDto accomSearchDto,
                           @RequestParam(value = "page") Optional<Integer> page,
                           Model model) {
        return renderTypePage(
                "category/pension",
                "/main/pensions",
                AccomType.PENSION,
                "펜션",
                "가족과 친구가 함께 머물기 좋은 펜션을 모았습니다.",
                accomSearchDto,
                page,
                model
        );
    }

    @GetMapping("/main/motels")
    public String motels(AccomSearchDto accomSearchDto,
                         @RequestParam(value = "page") Optional<Integer> page,
                         Model model) {
        return renderTypePage(
                "category/motel",
                "/main/motels",
                AccomType.MOTEL,
                "모텔",
                "합리적인 가격과 접근성을 갖춘 모텔을 빠르게 둘러보세요.",
                accomSearchDto,
                page,
                model
        );
    }

    @GetMapping("/main/guesthouses")
    public String guesthouses(AccomSearchDto accomSearchDto,
                              @RequestParam(value = "page") Optional<Integer> page,
                              Model model) {
        return renderTypePage(
                "category/guesthouse",
                "/main/guesthouses",
                AccomType.GUESTHOUSE,
                "게스트하우스",
                "가볍고 편안한 여행을 위한 게스트하우스를 소개합니다.",
                accomSearchDto,
                page,
                model
        );
    }

    private String renderTypePage(String viewName,
                                  String currentPath,
                                  AccomType accomType,
                                  String pageTitle,
                                  String pageDescription,
                                  AccomSearchDto accomSearchDto,
                                  Optional<Integer> page,
                                  Model model) {
        accomSearchDto.setAccomType(accomType);

        int currentPage = Math.max(page.orElse(1), 1);
        Pageable pageable = PageRequest.of(currentPage - 1, 5);
        Page<MainAccomDto> accomPage = accomService.getMainAccomPage(accomSearchDto, pageable);

        System.out.println("현재페이지 = " + accomPage.getNumber());
        System.out.println("총페이지 = " + accomPage.getTotalPages());
        System.out.println("총데이터 = " + accomPage.getTotalElements());


        model.addAttribute("accomList", accomPage.getContent());
        model.addAttribute("accomPage", accomPage);
        model.addAttribute("accomSearchDto", accomSearchDto);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("currentPath", currentPath);

        return viewName;
    }

    @GetMapping("/searchList")
    public String searchList(AccomSearchDto accomSearchDto,
                             @RequestParam(value = "page") Optional<Integer> page,
                             Model model) {
        int currentPage = Math.max(page.orElse(1), 1);
        Pageable pageable = PageRequest.of(currentPage - 1, 5);
        Page<MainAccomDto> accomPage = accomService.getMainAccomPage(accomSearchDto, pageable);

        model.addAttribute("accomList", accomPage.getContent());
        model.addAttribute("accomPage", accomPage);
        model.addAttribute("accomSearchDto", accomSearchDto);
        model.addAttribute("currentPath", "/searchList");

        return "category/searchList";
    }

    @GetMapping("/recent-viewed")
    public String recentViewed(@CookieValue(value = "recentViewedAccoms", required = false) String recentViewedCookie,
                               Model model) {
        List<Long> recentViewedIds = parseRecentViewedIds(recentViewedCookie);
        List<MainAccomDto> recentViewedList = accomService.getRecentViewedAccomList(recentViewedIds);

        model.addAttribute("recentViewedList", recentViewedList);
        model.addAttribute("recentViewedCount", recentViewedList.size());
        return "recent/recentViewedList";
    }

    private List<Long> parseRecentViewedIds(String recentViewedCookie) {
        if (recentViewedCookie == null || recentViewedCookie.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(recentViewedCookie.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .limit(20)
                .collect(Collectors.toList());
    }



}
