package com.Accommodation.controller;

import com.Accommodation.dto.AccomFormDto;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Review;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.ReviewService;
import com.Accommodation.validation.AccomValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AccomController {

    private final AccomService accomService;
    private final ReviewService reviewService;
    private final AccomValidator accomValidator;

    @Value("${naver.map.client.id}")
    private String naverMapClientId;

    @InitBinder("accomFormDto")
    public void initAccomFormDtoBinder(WebDataBinder binder) {
        binder.addValidators(accomValidator);
    }

    @GetMapping("/admin/accom/new")
    public String accomForm(@RequestParam(value = "returnPage", defaultValue = "0") int returnPage,
                            @RequestParam(value = "returnSearchQuery", required = false) String returnSearchQuery,
                            @RequestParam(value = "returnAccomType", required = false) String returnAccomType,
                            @RequestParam(value = "returnGrade", required = false) String returnGrade,
                            @RequestParam(value = "returnAccomStatus", required = false) String returnAccomStatus,
                            Model model) {
        model.addAttribute("accomFormDto", new AccomFormDto());
        model.addAttribute("currentPage", returnPage);
        model.addAttribute("returnSearchQuery", returnSearchQuery);
        model.addAttribute("returnAccomType", returnAccomType);
        model.addAttribute("returnGrade", returnGrade);
        model.addAttribute("returnAccomStatus", returnAccomStatus);
        return "accom/accomForm";
    }

    @PostMapping("/admin/accom/new")
    public String accomNew(@Valid @ModelAttribute AccomFormDto accomFormDto,
                           BindingResult bindingResult,
                           @RequestParam("accomImgFile") List<MultipartFile> accomImgFileList,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("accomFormDto", accomFormDto);
            return "accom/accomForm";
        }

        try {
            accomService.saveAccom(accomFormDto, accomImgFileList);
        } catch (Exception e) {
            model.addAttribute("accomFormDto", accomFormDto);
            model.addAttribute(
                    "errorMessage",
                    e.getMessage() == null || e.getMessage().isBlank()
                            ? "숙소 등록 중 오류가 발생했습니다."
                            : e.getMessage()
            );
            return "accom/accomForm";
        }

        return "redirect:/admin/accoms";
    }

    @GetMapping("/admin/accom/{accomId}")
    public String accomUpdateForm(@PathVariable("accomId") Long accomId,
                                  @RequestParam(value = "returnPage", defaultValue = "0") int returnPage,
                                  @RequestParam(value = "returnSearchQuery", required = false) String returnSearchQuery,
                                  @RequestParam(value = "returnAccomType", required = false) String returnAccomType,
                                  @RequestParam(value = "returnGrade", required = false) String returnGrade,
                                  @RequestParam(value = "returnAccomStatus", required = false) String returnAccomStatus,
                                  @RequestParam(value = "updated", required = false) Boolean updated,
                                  Model model) {
        try {
            AccomFormDto accomFormDto = accomService.getAccomFormDto(accomId);
            model.addAttribute("accomFormDto", accomFormDto);
            model.addAttribute("currentPage", returnPage);
            model.addAttribute("returnSearchQuery", returnSearchQuery);
            model.addAttribute("returnAccomType", returnAccomType);
            model.addAttribute("returnGrade", returnGrade);
            model.addAttribute("returnAccomStatus", returnAccomStatus);
            model.addAttribute("updated", Boolean.TRUE.equals(updated));
            return "accom/accomForm";
        } catch (EntityNotFoundException e) {
            return "redirect:/admin/accoms";
        }
    }

    @PostMapping("/admin/accom/{accomId}")
    public String accomUpdate(@PathVariable("accomId") Long accomId,
                              @Valid @ModelAttribute AccomFormDto accomFormDto,
                              BindingResult bindingResult,
                              @RequestParam(value = "returnPage", defaultValue = "0") int returnPage,
                              @RequestParam(value = "returnSearchQuery", required = false) String returnSearchQuery,
                              @RequestParam(value = "returnAccomType", required = false) String returnAccomType,
                              @RequestParam(value = "returnGrade", required = false) String returnGrade,
                              @RequestParam(value = "returnAccomStatus", required = false) String returnAccomStatus,
                              @RequestParam("accomImgFile") List<MultipartFile> accomImgFileList,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            accomFormDto.setId(accomId);
            model.addAttribute("accomFormDto", accomFormDto);
            model.addAttribute("currentPage", returnPage);
            model.addAttribute("returnSearchQuery", returnSearchQuery);
            model.addAttribute("returnAccomType", returnAccomType);
            model.addAttribute("returnGrade", returnGrade);
            model.addAttribute("returnAccomStatus", returnAccomStatus);
            return "accom/accomForm";
        }

        try {
            accomService.updateAccom(accomId, accomFormDto, accomImgFileList);
        } catch (Exception e) {
            accomFormDto.setId(accomId);
            model.addAttribute("accomFormDto", accomFormDto);
            model.addAttribute(
                    "errorMessage",
                    e.getMessage() == null || e.getMessage().isBlank()
                            ? "숙소 수정 중 오류가 발생했습니다."
                            : e.getMessage()
            );
            model.addAttribute("currentPage", returnPage);
            model.addAttribute("returnSearchQuery", returnSearchQuery);
            model.addAttribute("returnAccomType", returnAccomType);
            model.addAttribute("returnGrade", returnGrade);
            model.addAttribute("returnAccomStatus", returnAccomStatus);
            return "accom/accomForm";
        }

        redirectAttributes.addAttribute("page", returnPage);
        redirectAttributes.addAttribute("searchQuery", returnSearchQuery);
        redirectAttributes.addAttribute("accomType", returnAccomType);
        redirectAttributes.addAttribute("grade", returnGrade);
        redirectAttributes.addAttribute("accomStatus", returnAccomStatus);
        redirectAttributes.addAttribute("updated", true);
        return "redirect:/admin/accom/{accomId}";
    }

    @GetMapping("/admin/accoms")
    public String accomManage(AccomSearchDto accomSearchDto,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              Model model) {
        PageRequest pageRequest = PageRequest.of(page, 5);
        Page<Accom> accomPage = accomService.getAdminAccomPage(accomSearchDto, pageRequest);

        model.addAttribute("accomPage", accomPage);
        model.addAttribute("accomSearchDto", accomSearchDto);
        model.addAttribute("maxPage", 5);

        return "admin/accomMng";
    }

    @GetMapping("/accom/{accomId}")
    public String accomDtl(@PathVariable("accomId") Long accomId,
                           @AuthenticationPrincipal User user,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model) {
        Accom accom = accomService.getAccomDtl(accomId);
        updateRecentViewedCookie(accomId, request, response);

        ReviewFormDto reviewFormDto = new ReviewFormDto();
        reviewFormDto.setAccomId(accomId);

        Review myReview = null;
        if (user != null) {
            myReview = reviewService.getMyReview(accomId, user.getUsername());
        }

        boolean canWriteReview = user != null && reviewService.canWriteReview(accomId, user.getUsername());
        String reviewWriteDenyMessage = reviewService.getReviewWriteDenyMessage(
                accomId,
                user != null ? user.getUsername() : null
        );

        model.addAttribute("accom", accom);
        model.addAttribute("reviewList", reviewService.getReviewList(accomId));
        model.addAttribute("reviewFormDto", reviewFormDto);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("hasMyReview", myReview != null);
        model.addAttribute("myReview", myReview);
        model.addAttribute("canWriteReview", canWriteReview);
        model.addAttribute("reviewWriteDenyMessage", reviewWriteDenyMessage);
        model.addAttribute("naverMapClientId", naverMapClientId);

        return "accom/accomDtl";
    }

    private void updateRecentViewedCookie(Long accomId,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        List<String> viewedIds = new ArrayList<>();
        viewedIds.add(String.valueOf(accomId));

        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> "recentViewedAccoms".equals(cookie.getName()))
                    .findFirst()
                    .ifPresent(cookie -> Arrays.stream(cookie.getValue().split(","))
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .filter(value -> !value.equals(String.valueOf(accomId)))
                            .limit(19)
                            .forEach(viewedIds::add));
        }

        Cookie cookie = new Cookie("recentViewedAccoms", viewedIds.stream().collect(Collectors.joining(",")));
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);
    }

    @GetMapping("/admin/accom/delete/{accomId}")
    public String deleteAccom(@PathVariable("accomId") Long accomId,
                              @RequestParam(value = "returnPage", defaultValue = "0") int returnPage,
                              @RequestParam(value = "returnSearchQuery", required = false) String returnSearchQuery,
                              @RequestParam(value = "returnAccomType", required = false) String returnAccomType,
                              @RequestParam(value = "returnGrade", required = false) String returnGrade,
                              @RequestParam(value = "returnAccomStatus", required = false) String returnAccomStatus,
                              RedirectAttributes redirectAttributes) {
        accomService.deleteAccom(accomId);
        redirectAttributes.addAttribute("page", returnPage);
        redirectAttributes.addAttribute("searchQuery", returnSearchQuery);
        redirectAttributes.addAttribute("accomType", returnAccomType);
        redirectAttributes.addAttribute("grade", returnGrade);
        redirectAttributes.addAttribute("accomStatus", returnAccomStatus);
        return "redirect:/admin/accoms";
    }
}
