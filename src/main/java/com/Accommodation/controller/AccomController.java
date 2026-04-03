package com.Accommodation.controller;

import com.Accommodation.dto.AccomFormDto;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Review;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
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
    public String accomNew(@Valid @ModelAttribute AccomFormDto accomFormDto,
                           BindingResult bindingResult,
                           @RequestParam("accomImgFile") List<MultipartFile> accomImgFileList,
                           Model model) {
        validateGuestCount(accomFormDto, bindingResult);
        validateOperationInfo(accomFormDto, bindingResult);

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
                            ? "숙소 등록 중 오류가 발생하였습니다."
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
        validateGuestCount(accomFormDto, bindingResult);
        validateOperationInfo(accomFormDto, bindingResult);

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
                            ? "숙소 수정 중 오류가 발생하였습니다."
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

    private void validateGuestCount(AccomFormDto accomFormDto, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("accomType") || bindingResult.hasFieldErrors("guestCount")) {
            return;
        }

        if (accomFormDto.getAccomType() == null || accomFormDto.getGuestCount() == null) {
            return;
        }

        switch (accomFormDto.getAccomType()) {
            case HOTEL, RESORT, PENSION -> {
                if (accomFormDto.getGuestCount() < 2 || accomFormDto.getGuestCount() > 10) {
                    bindingResult.rejectValue(
                            "guestCount",
                            "guestCount.range",
                            "호텔, 리조트, 펜션은 투숙 가능 인원을 2명에서 10명 사이로 입력해 주세요."
                    );
                }
            }
            case GUESTHOUSE, MOTEL -> {
                if (accomFormDto.getGuestCount() < 1 || accomFormDto.getGuestCount() > 6) {
                    bindingResult.rejectValue(
                            "guestCount",
                            "guestCount.range",
                            "게스트하우스와 모텔은 투숙 가능 인원을 1명에서 6명 사이로 입력해 주세요."
                    );
                }
            }
        }
    }

    private void validateOperationInfo(AccomFormDto accomFormDto, BindingResult bindingResult) {
        if (!bindingResult.hasFieldErrors("operationStartDate")
                && !bindingResult.hasFieldErrors("operationEndDate")
                && accomFormDto.getOperationStartDate() != null
                && accomFormDto.getOperationEndDate() != null) {

            if (accomFormDto.getOperationStartDate().isAfter(accomFormDto.getOperationEndDate())) {
                bindingResult.rejectValue(
                        "operationEndDate",
                        "operationEndDate.range",
                        "운영 종료일은 운영 시작일보다 빠를 수 없습니다."
                );
            }
        }

        if (!bindingResult.hasFieldErrors("checkInTime")
                && !bindingResult.hasFieldErrors("checkOutTime")
                && accomFormDto.getCheckInTime() != null
                && accomFormDto.getCheckOutTime() != null) {

            if (accomFormDto.getCheckInTime().equals(accomFormDto.getCheckOutTime())) {
                bindingResult.rejectValue(
                        "checkOutTime",
                        "checkOutTime.duplicate",
                        "체크인 시간과 체크아웃 시간은 같을 수 없습니다."
                );
            }
        }

        if (accomFormDto.getOperationDateList() == null || accomFormDto.getOperationDateList().isEmpty()) {
            bindingResult.rejectValue(
                    "operationDateList",
                    "operationDateList.empty",
                    "운영일을 한 개 이상 선택해 주세요."
            );
            return;
        }

        if (accomFormDto.getOperationStartDate() == null || accomFormDto.getOperationEndDate() == null) {
            return;
        }

        for (String operationDateStr : accomFormDto.getOperationDateList()) {
            try {
                LocalDate operationDate = LocalDate.parse(operationDateStr);

                if (operationDate.isBefore(accomFormDto.getOperationStartDate())
                        || operationDate.isAfter(accomFormDto.getOperationEndDate())) {
                    bindingResult.rejectValue(
                            "operationDateList",
                            "operationDateList.range",
                            "운영일은 운영 기간 안에서만 선택해 주세요."
                    );
                    break;
                }
            } catch (Exception e) {
                bindingResult.rejectValue(
                        "operationDateList",
                        "operationDateList.format",
                        "운영일 형식이 올바르지 않습니다."
                );
                break;
            }
        }
    }
}