package com.Accommodation.controller;

import com.Accommodation.dto.ChatbotActivityItemDto;
import com.Accommodation.dto.ChatbotAssistantResponseDto;
import com.Accommodation.dto.ChatbotComparisonResponseDto;
import com.Accommodation.dto.ChatbotRecommendationResponseDto;
import com.Accommodation.dto.ChatbotSelectableAccomDto;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.ChatbotAiService;
import com.Accommodation.service.RegionActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final AccomService accomService;
    private final RegionActivityService regionActivityService;
    private final ChatbotAiService chatbotAiService;

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

    @GetMapping("/cart-candidates")
    public ChatbotAssistantResponseDto cartCandidates(
            @RequestParam("location") String location,
            @RequestParam("accomType") String accomType,
            @RequestParam("checkInDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam("checkOutDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam("adultCount") int adultCount,
            @RequestParam(value = "childCount", defaultValue = "0") int childCount,
            @RequestParam(value = "roomCount", defaultValue = "1") int roomCount,
            Principal principal) {
        return chatbotAiService.previewCartCandidates(
                principal != null ? principal.getName() : null,
                location,
                accomType,
                checkInDate,
                checkOutDate,
                adultCount,
                childCount,
                roomCount
        );
    }

    @GetMapping("/activities")
    public List<ChatbotActivityItemDto> activities(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "region", defaultValue = "서울") String region) {
        return regionActivityService.getChatbotActivities(keyword, region);
    }

    @GetMapping("/assistant")
    public ChatbotAssistantResponseDto assistant(
            @RequestParam("message") String message,
            Principal principal,
            @CookieValue(value = "recentViewedAccoms", required = false) String recentViewedCookie) {
        return chatbotAiService.chat(message, principal != null ? principal.getName() : null, recentViewedCookie);
    }

    @GetMapping(value = "/assistant/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter assistantStream(
            @RequestParam("message") String message,
            Principal principal,
            @CookieValue(value = "recentViewedAccoms", required = false) String recentViewedCookie) {
        return chatbotAiService.streamChat(message, principal != null ? principal.getName() : null, recentViewedCookie);
    }
}
