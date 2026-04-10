package com.Accommodation.controller;

import com.Accommodation.dto.ChatbotComparisonResponseDto;
import com.Accommodation.dto.ChatbotRecommendationResponseDto;
import com.Accommodation.dto.ChatbotSelectableAccomDto;
import com.Accommodation.service.AccomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final AccomService accomService;

    @GetMapping("/recommendations")
    public ChatbotRecommendationResponseDto recommendations(@RequestParam("query") String query) {
        return accomService.getChatbotRecommendations(query);
    }

    @GetMapping("/selectable-accoms")
    public List<ChatbotSelectableAccomDto> selectableAccoms(
            Principal principal,
            @CookieValue(value = "recentViewedAccoms", required = false) String recentViewedCookie) {
        String email = principal != null ? principal.getName() : null;
        return accomService.getSelectableAccoms(email, recentViewedCookie);
    }

    @GetMapping("/compare")
    public ChatbotComparisonResponseDto compare(
            @RequestParam("leftId") Long leftId,
            @RequestParam("rightId") Long rightId) {
        return accomService.compareChatbotAccoms(leftId, rightId);
    }
}
