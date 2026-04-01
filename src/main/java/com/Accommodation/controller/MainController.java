package com.Accommodation.controller;

import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.service.AccomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

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
}