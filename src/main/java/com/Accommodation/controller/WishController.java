package com.Accommodation.controller;

import com.Accommodation.dto.ActivityWishDto;
import com.Accommodation.dto.WishListDto;
import com.Accommodation.service.ActivityWishService;
import com.Accommodation.service.WishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;
    private final ActivityWishService activityWishService;

    @GetMapping("/wish")
    public String wishList(@RequestParam(value = "sort", defaultValue = "priceDesc") String sort,
                           Model model,
                           Principal principal) {
        List<WishListDto> wishItems = wishService.getWishList(principal.getName(), sort);
        List<ActivityWishDto> activityWishItems = activityWishService.getWishList(principal.getName());
        int totalWishCount = wishItems.size() + activityWishItems.size();
        model.addAttribute("wishItems", wishItems);
        model.addAttribute("activityWishItems", activityWishItems);
        model.addAttribute("wishCount", wishItems.size());
        model.addAttribute("activityWishCount", activityWishItems.size());
        model.addAttribute("totalWishCount", totalWishCount);
        model.addAttribute("selectedSort", sort);
        return "wish/wishList";
    }

    @PostMapping("/wish/{accomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addWish(@PathVariable Long accomId, Principal principal) {
        ensureAuthenticated(principal);
        wishService.addWish(accomId, principal.getName());
        return ResponseEntity.ok(Map.of(
                "wished", true,
                "wishCount", wishService.getWishCount(principal.getName())
        ));
    }

    @DeleteMapping("/wish/{accomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeWish(@PathVariable Long accomId, Principal principal) {
        ensureAuthenticated(principal);
        wishService.removeWish(accomId, principal.getName());
        return ResponseEntity.ok(Map.of(
                "wished", false,
                "wishCount", wishService.getWishCount(principal.getName())
        ));
    }

    private void ensureAuthenticated(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
