package com.Accommodation.controller;

import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Review;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AccomService accomService;
    private final MemberRepository memberRepository;

    @GetMapping("/review/accom/{accomId}")
    public String reviewPage(@PathVariable("accomId") Long accomId,
                             @AuthenticationPrincipal User user,
                             Model model) {

        Accom accom = accomService.getAccomDtl(accomId);
        model.addAttribute("accom", accom);
        model.addAttribute("reviewList", reviewService.getReviewList(accomId));

        ReviewFormDto reviewFormDto = new ReviewFormDto();
        reviewFormDto.setAccomId(accomId);
        model.addAttribute("reviewFormDto", reviewFormDto);

        boolean hasMyReview = false;
        Review myReview = null;
        Long loginMemberId = null;

        if (user != null) {
            hasMyReview = reviewService.hasMyReview(accomId, user.getUsername());
            myReview = reviewService.getMyReview(accomId, user.getUsername());

            Member member = memberRepository.findByEmail(user.getUsername());
            if (member != null) {
                loginMemberId = member.getId();
            }
        }

        model.addAttribute("hasMyReview", hasMyReview);
        model.addAttribute("myReview", myReview);
        model.addAttribute("loginMemberId", loginMemberId);

        return "review/review";
    }

    @PostMapping("/reviews/new")
    @ResponseBody
    public Map<String, Object> saveReview(@Valid @ModelAttribute ReviewFormDto reviewFormDto,
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
    @ResponseBody
    public Map<String, Object> updateReview(@PathVariable("reviewId") Long reviewId,
                                            @ModelAttribute ReviewFormDto reviewFormDto,
                                            @AuthenticationPrincipal User user) {

        Map<String, Object> result = new HashMap<>();

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
    @ResponseBody
    public Map<String, Object> deleteReview(@PathVariable("reviewId") Long reviewId,
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

    @PostMapping("/reviews/{reviewId}/images/{reviewImgId}/delete")
    @ResponseBody
    public Map<String, Object> deleteReviewImage(@PathVariable("reviewId") Long reviewId,
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