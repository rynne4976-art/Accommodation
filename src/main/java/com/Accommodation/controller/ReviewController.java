package com.Accommodation.controller;

import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Review;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AccomService accomService;

    @GetMapping("/reviews/accom/{accomId}")
    public String reviewList(@PathVariable("accomId") Long accomId,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             Model model) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), 5);
        Page<Review> reviewPage = reviewService.getReviewPage(accomId, pageable);

        model.addAttribute("accom", accomService.getAccomDtl(accomId));
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("reviewList", reviewPage.getContent());
        model.addAttribute("currentPath", "/reviews/accom/" + accomId);
        return "review/reviewList";
    }

    @PostMapping("/reviews/new")
    public String saveReview(@Valid @ModelAttribute ReviewFormDto reviewFormDto,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "입력값을 확인해 주세요.");
            return "redirect:/accom/" + reviewFormDto.getAccomId() + "#review-info";
        }

        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "로그인 후 리뷰를 작성할 수 있습니다.");
            return "redirect:/accom/" + reviewFormDto.getAccomId() + "#review-info";
        }

        try {
            reviewService.saveReview(reviewFormDto, user.getUsername());
            redirectAttributes.addFlashAttribute("message", "리뷰가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }

        return "redirect:/accom/" + reviewFormDto.getAccomId() + "#review-info";
    }

    @PostMapping("/reviews/new/ajax")
    @ResponseBody
    public Map<String, Object> saveReviewAjax(@Valid @ModelAttribute ReviewFormDto reviewFormDto,
                                              BindingResult bindingResult,
                                              @AuthenticationPrincipal User user) {

        Map<String, Object> result = new HashMap<>();

        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", "입력값을 확인해 주세요.");
            return result;
        }

        if (user == null) {
            result.put("success", false);
            result.put("message", "로그인 후 리뷰를 작성할 수 있습니다.");
            return result;
        }

        try {
            reviewService.saveReview(reviewFormDto, user.getUsername());
            result.put("success", true);
            result.put("message", "리뷰가 등록되었습니다.");
            result.put("accomId", reviewFormDto.getAccomId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/reviews/{reviewId}/update")
    public String updateReview(@PathVariable("reviewId") Long reviewId,
                               @ModelAttribute ReviewFormDto reviewFormDto,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {

        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "로그인 후 리뷰를 수정할 수 있습니다.");
            return "redirect:/accom/" + reviewFormDto.getAccomId() + "#review-info";
        }

        try {
            reviewService.updateReview(reviewId, reviewFormDto, user.getUsername());
            redirectAttributes.addFlashAttribute("message", "리뷰가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }

        return "redirect:/accom/" + reviewFormDto.getAccomId() + "#review-info";
    }

    @PostMapping("/reviews/{reviewId}/update/ajax")
    @ResponseBody
    public Map<String, Object> updateReviewAjax(@PathVariable("reviewId") Long reviewId,
                                                @Valid @ModelAttribute ReviewFormDto reviewFormDto,
                                                BindingResult bindingResult,
                                                @AuthenticationPrincipal User user) {

        Map<String, Object> result = new HashMap<>();

        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", "입력값을 확인해 주세요.");
            return result;
        }

        if (user == null) {
            result.put("success", false);
            result.put("message", "로그인 후 리뷰를 수정할 수 있습니다.");
            return result;
        }

        try {
            reviewService.updateReview(reviewId, reviewFormDto, user.getUsername());
            result.put("success", true);
            result.put("message", "리뷰가 수정되었습니다.");
            result.put("accomId", reviewFormDto.getAccomId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable("reviewId") Long reviewId,
                               @RequestParam("accomId") Long accomId,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {

        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "로그인 후 리뷰를 삭제할 수 있습니다.");
            return "redirect:/accom/" + accomId + "#review-info";
        }

        try {
            reviewService.deleteReview(reviewId, user.getUsername());
            redirectAttributes.addFlashAttribute("message", "리뷰가 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }

        return "redirect:/accom/" + accomId + "#review-info";
    }

    @PostMapping("/reviews/{reviewId}/delete/ajax")
    @ResponseBody
    public Map<String, Object> deleteReviewAjax(@PathVariable("reviewId") Long reviewId,
                                                @RequestParam("accomId") Long accomId,
                                                @AuthenticationPrincipal User user) {

        Map<String, Object> result = new HashMap<>();

        if (user == null) {
            result.put("success", false);
            result.put("message", "로그인 후 리뷰를 삭제할 수 있습니다.");
            return result;
        }

        try {
            reviewService.deleteReview(reviewId, user.getUsername());
            result.put("success", true);
            result.put("message", "리뷰가 삭제되었습니다.");
            result.put("accomId", accomId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/reviews/{reviewId}/images/{reviewImgId}/delete/ajax")
    @ResponseBody
    public Map<String, Object> deleteReviewImageAjax(@PathVariable("reviewId") Long reviewId,
                                                     @PathVariable("reviewImgId") Long reviewImgId,
                                                     @RequestParam("accomId") Long accomId,
                                                     @AuthenticationPrincipal User user) {

        Map<String, Object> result = new HashMap<>();

        if (user == null) {
            result.put("success", false);
            result.put("message", "로그인 후 리뷰 이미지를 삭제할 수 있습니다.");
            return result;
        }

        try {
            reviewService.deleteReviewImage(reviewId, reviewImgId, user.getUsername());
            result.put("success", true);
            result.put("message", "리뷰 이미지가 삭제되었습니다.");
            result.put("accomId", accomId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }
}
