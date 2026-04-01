package com.Accommodation.controller;

import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AccomService accomService;

    @PostMapping("/review/new")
    public String saveReview(@Valid @ModelAttribute("reviewFormDto")ReviewFormDto reviewFormDto,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal User user,
                             Model model){
        Accom accom = accomService.getAccomDtl(reviewFormDto.getAccomId());
        model.addAttribute("accom", accom);
        model.addAttribute("reviewList", reviewService.getReviewList(reviewFormDto.getAccomId()));

        boolean hasMyReview = false;
        if(user != null){
            hasMyReview = reviewService.hasMyReview(reviewFormDto.getAccomId(), user.getUsername());
        }
        model.addAttribute("hasMyReview", hasMyReview);

        if(bindingResult.hasErrors()){
            return "accom/accomDtl";
        }

        if(user == null){
            model.addAttribute("errorMessage", "로그인 후 리뷰를 작성할 수 있습니다.");
            return "accom/accomDtl";
        }

        try {
            reviewService.saveReview(reviewFormDto, user.getUsername());
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
            return "accom/accomDtl";
        }

        return "redirect:/accom" + reviewFormDto.getAccomId();
    }

}
