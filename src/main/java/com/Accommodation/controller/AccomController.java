package com.Accommodation.controller;

import com.Accommodation.dto.AccomFormDto;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Review;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AccomController {

    private final AccomService accomService;
    private final ReviewService reviewService;

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
    public String accomNew(@ModelAttribute AccomFormDto accomFormDto,
                           @RequestParam("accomImgFile") List<MultipartFile> accomImgFileList,
                           Model model) {
        try {
            accomService.saveAccom(accomFormDto, accomImgFileList);
        } catch (Exception e) {
            model.addAttribute("accomFormDto", accomFormDto);
            model.addAttribute("errorMessage", "숙소 등록 중 오류가 발생하였습니다.");
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
                                  Model model) {
        try {
            AccomFormDto accomFormDto = accomService.getAccomFormDto(accomId);
            model.addAttribute("accomFormDto", accomFormDto);
            model.addAttribute("currentPage", returnPage);
            model.addAttribute("returnSearchQuery", returnSearchQuery);
            model.addAttribute("returnAccomType", returnAccomType);
            model.addAttribute("returnGrade", returnGrade);
            model.addAttribute("returnAccomStatus", returnAccomStatus);
            return "accom/accomForm";
        } catch (EntityNotFoundException e) {
            return "redirect:/admin/accoms";
        }
    }

    @PostMapping("/admin/accom/{accomId}")
    public String accomUpdate(@PathVariable("accomId") Long accomId,
                              @ModelAttribute AccomFormDto accomFormDto,
                              @RequestParam(value = "returnPage", defaultValue = "0") int returnPage,
                              @RequestParam(value = "returnSearchQuery", required = false) String returnSearchQuery,
                              @RequestParam(value = "returnAccomType", required = false) String returnAccomType,
                              @RequestParam(value = "returnGrade", required = false) String returnGrade,
                              @RequestParam(value = "returnAccomStatus", required = false) String returnAccomStatus,
                              @RequestParam("accomImgFile") List<MultipartFile> accomImgFileList,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            accomService.updateAccom(accomId, accomFormDto, accomImgFileList);
        } catch (Exception e) {
            accomFormDto.setId(accomId);
            model.addAttribute("accomFormDto", accomFormDto);
            model.addAttribute("errorMessage", "숙소 수정 중 오류가 발생하였습니다.");
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
        return "redirect:/admin/accoms";
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
                           Model model) {

        Accom accom = accomService.getAccomDtl(accomId);

        ReviewFormDto reviewFormDto = new ReviewFormDto();
        reviewFormDto.setAccomId(accomId);

        Review myReview = null;

        if (user != null) {
            myReview = reviewService.getMyReview(accomId, user.getUsername());
        }

        model.addAttribute("accom", accom);
        model.addAttribute("reviewList", reviewService.getReviewList(accomId));
        model.addAttribute("reviewFormDto", reviewFormDto);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("hasMyReview", myReview != null);
        model.addAttribute("myReview", myReview);

        return "accom/accomDtl";
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
