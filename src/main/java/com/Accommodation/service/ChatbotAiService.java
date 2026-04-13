package com.Accommodation.service;

import com.Accommodation.constant.AccomType;
import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.ChatbotAssistantResponseDto;
import com.Accommodation.dto.ChatbotRecommendationItemDto;
import com.Accommodation.dto.ChatbotRecommendationResponseDto;
import com.Accommodation.dto.CartItemDto;
import com.Accommodation.dto.CartListItemDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.dto.NotificationDto;
import com.Accommodation.dto.OrderHistDto;
import com.Accommodation.dto.OrderItemDto;
import com.Accommodation.dto.WishListDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.util.GuestPricingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotAiService {

    private final RestClient.Builder restClientBuilder;
    private final AccomService accomService;
    private final AccomRepository accomRepository;
    private final CartService cartService;
    private final WishService wishService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final RegionActivityService regionActivityService;
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.base-url:https://openrouter.ai/api}")
    private String baseUrl;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:openrouter/free}")
    private String model;

    @Value("${ai.openai.max-output-tokens:400}")
    private int maxOutputTokens;

    @Value("${ai.openai.site-url:}")
    private String siteUrl;

    @Value("${ai.openai.app-name:Accommodation}")
    private String appName;

    public ChatbotAssistantResponseDto chat(String message, String email, String recentViewedCookie) {
        String safeMessage = message == null ? "" : message.trim();
        ChatbotRecommendationResponseDto recommendationResponse = accomService.getChatbotRecommendations(safeMessage);
        List<ChatbotRecommendationItemDto> recommendations = recommendationResponse.getRecommendations();
        String assistantMessage = recommendationResponse.getAssistantMessage();

        if (safeMessage.isBlank()) {
            return new ChatbotAssistantResponseDto("질문을 입력해 주세요.", recommendations);
        }

        ChatbotAssistantResponseDto actionResponse = tryHandleActionRequest(safeMessage, email, recentViewedCookie, recommendations);
        if (actionResponse != null) {
            return actionResponse;
        }

        if (assistantMessage != null && !assistantMessage.isBlank()) {
            return new ChatbotAssistantResponseDto(assistantMessage, recommendations);
        }

        if (apiKey == null || apiKey.isBlank()) {
            return new ChatbotAssistantResponseDto(buildFallbackAnswer(recommendations), recommendations);
        }

        try {
            RestClient.Builder builder = restClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            if (siteUrl != null && !siteUrl.isBlank()) {
                builder.defaultHeader("HTTP-Referer", siteUrl);
            }
            if (appName != null && !appName.isBlank()) {
                builder.defaultHeader("X-Title", appName);
            }

            RestClient restClient = builder.build();

            JsonNode response = restClient.post()
                    .uri("/api/v1/chat/completions")
                    .body(buildRequestBody(safeMessage, recommendations))
                    .retrieve()
                    .body(JsonNode.class);

            String rawText = extractMessageContent(response);
            AiAssistantPayload payload = parsePayload(rawText);

            String answer = resolveAnswer(payload, rawText, recommendations);

            List<ChatbotRecommendationItemDto> refinedRecommendations = payload != null
                    ? applyAiReasons(recommendations, payload.recommendations())
                    : recommendations;

            if (refinedRecommendations == null || refinedRecommendations.isEmpty()) {
                refinedRecommendations = recommendations;
            }

            return new ChatbotAssistantResponseDto(answer, refinedRecommendations);
        } catch (RestClientResponseException e) {
            log.warn("OpenAI API call failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("OpenAI API call failed", e);
        }

        return new ChatbotAssistantResponseDto(buildFallbackAnswer(recommendations), recommendations);
    }

    public ChatbotAssistantResponseDto previewCartCandidates(
            String email,
            String locationKeyword,
            String accomTypeLabel,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int adultCount,
            int childCount,
            int roomCount) {
        if (email == null || email.isBlank()) {
            return new ChatbotAssistantResponseDto("장바구니 담기는 로그인 후 사용할 수 있습니다. 로그인한 뒤 다시 시도해 주세요.", List.of(), "login_required", null);
        }

        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            return new ChatbotAssistantResponseDto("체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.", List.of(), "cart_candidate_empty", null);
        }

        ParsedCartRequest parsed = buildCartRequest(
                locationKeyword,
                accomTypeLabel,
                checkInDate,
                checkOutDate,
                adultCount,
                childCount,
                roomCount
        );

        List<MainAccomDto> candidates = findCartCandidates(parsed);
        if (candidates.isEmpty()) {
            return new ChatbotAssistantResponseDto("조건에 맞는 숙소를 찾지 못했습니다. 지역이나 숙박 조건을 조금 완화해서 다시 시도해 주세요.", List.of(), "cart_candidate_empty", null);
        }

        List<ChatbotRecommendationItemDto> validCandidates = new ArrayList<>();
        String lastFailureReason = null;
        String lastFailureAccomName = null;

        for (MainAccomDto candidate : candidates) {
            try {
                validateCartCandidate(candidate.getId(), parsed);
                validCandidates.add(toRecommendationItem(candidate));
                if (validCandidates.size() >= 3) {
                    break;
                }
            } catch (Exception e) {
                log.info("AI cart preview skipped for accomId {}: {}", candidate.getId(), e.getMessage());
                lastFailureAccomName = candidate.getAccomName();
                lastFailureReason = normalizeCartFailureReason(e.getMessage());
            }
        }

        if (validCandidates.isEmpty()) {
            String reason = lastFailureReason != null && !lastFailureReason.isBlank()
                    ? " 사유: " + (lastFailureAccomName != null ? lastFailureAccomName + " - " : "") + lastFailureReason
                    : "";
            return new ChatbotAssistantResponseDto("조건에 맞는 숙소를 찾았지만 장바구니에 담을 수 있는 업소가 없습니다." + reason, List.of(), "cart_candidate_empty", null);
        }

        if (validCandidates.size() == 1) {
            ChatbotRecommendationItemDto selected = validCandidates.get(0);
            return new ChatbotAssistantResponseDto(
                    selected.getAccomName() + " 조건이 가장 잘 맞습니다. 바로 장바구니에 담겠습니다.",
                    validCandidates,
                    "cart_candidate_single",
                    selected.getAccomId()
            );
        }

        return new ChatbotAssistantResponseDto(
                "조건에 맞는 숙박 업소가 여러 개 있습니다. 장바구니에 담을 숙박 업소를 선택해 주세요.",
                validCandidates,
                "cart_candidate_select",
                null
        );
    }

    public SseEmitter streamChat(String message, String email, String recentViewedCookie) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                ChatbotAssistantResponseDto response = chat(message, email, recentViewedCookie);
                for (String chunk : splitAnswer(response.getAnswer())) {
                    emit(emitter, "chunk", chunk);
                    sleepBriefly();
                }
                Map<String, Object> actionPayload = new java.util.LinkedHashMap<>();
                actionPayload.put("actionType", response.getActionType());
                actionPayload.put("selectedAccomId", response.getSelectedAccomId());
                emit(emitter, "action", objectMapper.writeValueAsString(actionPayload));
                emit(emitter, "recommendations", objectMapper.writeValueAsString(response.getRecommendations()));
                emit(emitter, "done", "done");
                emitter.complete();
            } catch (Exception e) {
                log.warn("Failed to stream chatbot response", e);
                try {
                    emit(emitter, "error", "AI 응답을 스트리밍하지 못했습니다.");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onCompletion(() -> {
        });
        return emitter;
    }

    private Map<String, Object> buildRequestBody(String message, List<ChatbotRecommendationItemDto> recommendations) {
        String systemPrompt = """
                You are a Korean AI assistant for an accommodation booking service.
                Use only the candidate accommodations provided below.
                Do not invent facts that are not present in the candidate list.
                Return valid JSON only.
                The JSON schema is:
                {
                  "answer": "string",
                  "recommendations": [
                    {
                      "accomId": number,
                      "reasons": ["string", "string"]
                    }
                  ]
                }
                Keep answer concise in Korean.
                Rewrite each reason naturally in Korean for the user's question.
                """;

        String userPrompt = """
                사용자 질문:
                %s

                추천 후보:
                %s
                """.formatted(message, buildRecommendationContext(recommendations));

        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                ),
                "max_tokens", maxOutputTokens,
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );
    }

    private ChatbotAssistantResponseDto tryHandleActionRequest(
            String message,
            String email,
            String recentViewedCookie,
            List<ChatbotRecommendationItemDto> recommendations) {
        ChatbotAssistantResponseDto response = tryHandleComparisonRequest(message, recommendations);
        if (response != null) {
            return response;
        }

        response = tryHandleTripPlanRequest(message);
        if (response != null) {
            return response;
        }

        response = tryHandleCartRequest(message, email, recommendations);
        if (response != null) {
            return response;
        }

        response = tryHandleWishRequest(message, email, recommendations);
        if (response != null) {
            return response;
        }

        response = tryHandleRecentViewedRequest(message, recentViewedCookie);
        if (response != null) {
            return response;
        }

        response = tryHandleReservationGuideRequest(message, recommendations);
        if (response != null) {
            return response;
        }

        response = tryHandleOrderRequest(message, email);
        if (response != null) {
            return response;
        }

        response = tryHandleNotificationRequest(message, email);
        if (response != null) {
            return response;
        }

        return null;
    }

    private ChatbotAssistantResponseDto tryHandleTripPlanRequest(String message) {
        if (!containsAny(
                message,
                "여행 일정", "여행일정", "여행 계획", "여행계획",
                "일정 짜", "일정짜", "일정 만들", "일정만들",
                "계획 짜", "계획짜", "계획 세워", "계획세워", "계획 만들", "계획만들",
                "여행 짜", "여행짜", "여행 만들", "여행만들",
                "플랜 짜", "플랜짜", "플랜 만들", "플랜만들",
                "코스 짜", "코스짜", "코스 만들", "코스만들"
        )) {
            return null;
        }

        ParsedCartRequest parsed = parseCartRequest(message);
        if (parsed == null || parsed.locationKeyword() == null || parsed.checkInDate() == null || parsed.checkOutDate() == null) {
            return new ChatbotAssistantResponseDto("여행 일정을 짜려면 지역, 체크인 날짜, 숙박 일수를 함께 알려주세요.", List.of());
        }

        List<ChatbotRecommendationItemDto> stayRecommendations = findCartCandidates(parsed).stream()
                .limit(3)
                .map(this::toRecommendationItem)
                .toList();

        String answer = stayRecommendations.isEmpty()
                ? "해당 조건에 맞는 숙소가 없습니다."
                : parsed.locationKeyword() + " " + parsed.checkInDate() + "부터 " + parsed.checkOutDate()
                + "까지 일정에 맞는 숙소와 즐길거리를 추천합니다.";

        return new ChatbotAssistantResponseDto(
                answer,
                stayRecommendations,
                "trip_plan_ready",
                null,
                parsed.locationKeyword(),
                parsed.checkInDate(),
                parsed.checkOutDate()
        );
    }

    private ChatbotAssistantResponseDto tryHandleReservationGuideRequest(
            String message,
            List<ChatbotRecommendationItemDto> recommendations) {
        if (!message.contains("예약")) {
            return null;
        }

        if (containsAny(message, "예약 내역", "예약내역", "예약 목록", "예약목록", "예약 취소", "주문 취소", "주문")) {
            return null;
        }

        if (containsAny(message, "장바구니", "카트")) {
            return null;
        }

        ParsedCartRequest parsed = parseCartRequestWithOptionalDates(message);
        List<ChatbotRecommendationItemDto> filteredRecommendations = recommendations;
        String locationTerm = accomService.extractRequestedLocationTerm(message).orElse(null);

        if (parsed == null || parsed.locationKeyword() == null) {
            if (locationTerm != null && !locationTerm.isBlank()) {
                return new ChatbotAssistantResponseDto(locationTerm + " 지역에는 등록된 숙소가 없습니다.", List.of());
            }
            return new ChatbotAssistantResponseDto("입력한 지역에는 등록된 숙소가 없습니다.", List.of());
        }

        if (parsed.accomType() != null || parsed.locationKeyword() != null) {
            filteredRecommendations = findCartCandidates(parsed).stream()
                    .limit(5)
                    .map(this::toRecommendationItem)
                    .toList();
        }

        if (filteredRecommendations.isEmpty()) {
            return new ChatbotAssistantResponseDto("해당 조건에 맞는 숙소가 없습니다.", List.of());
        }

        MainAccomDto target = findAccomTargetFromMessage(message, filteredRecommendations);
        String accomName = target != null && target.getAccomName() != null && !target.getAccomName().isBlank()
                ? target.getAccomName()
                : parsed.locationKeyword() + " 숙소";

        return new ChatbotAssistantResponseDto(accomName + " 장바구니 담기를 우선 진행해 주세요.", filteredRecommendations);
    }

    private ChatbotAssistantResponseDto tryHandleComparisonRequest(
            String message,
            List<ChatbotRecommendationItemDto> recommendations) {
        if (!containsAny(message, "비교")) {
            return null;
        }

        List<MainAccomDto> allAccoms = accomService.getMainAccomPage(new AccomSearchDto(), PageRequest.of(0, 200)).getContent();
        List<MainAccomDto> matched = allAccoms.stream()
                .filter(item -> containsNormalized(message, item.getAccomName()))
                .sorted(Comparator.comparingInt((MainAccomDto item) -> item.getAccomName().length()).reversed())
                .distinct()
                .limit(2)
                .toList();

        if (matched.size() < 2) {
            return new ChatbotAssistantResponseDto("비교할 숙소 두 곳의 이름을 함께 입력해 주세요.", recommendations);
        }

        List<ChatbotRecommendationItemDto> comparisonTargets = matched.stream()
                .map(this::toRecommendationItem)
                .toList();

        return new ChatbotAssistantResponseDto(
                matched.get(0).getAccomName() + "와(과) " + matched.get(1).getAccomName() + " 비교를 진행합니다.",
                comparisonTargets,
                "compare_ready",
                null
        );
    }

    private ChatbotAssistantResponseDto tryHandleCartRequest(
            String message,
            String email,
            List<ChatbotRecommendationItemDto> recommendations) {
        if (!containsAny(message, "장바구니", "카트")) {
            return null;
        }

        if (email == null || email.isBlank()) {
            return new ChatbotAssistantResponseDto("장바구니 담기는 로그인 후 사용할 수 있습니다. 로그인한 뒤 다시 요청해 주세요.", recommendations);
        }

        if (containsAny(message, "보여", "조회", "목록", "확인")) {
            List<CartListItemDto> cartItems = cartService.getCartItems(email);
            if (cartItems.isEmpty()) {
                return new ChatbotAssistantResponseDto("장바구니가 비어 있습니다.", List.of(), "cart_list", null);
            }
            return new ChatbotAssistantResponseDto(
                    "장바구니에 담긴 숙소 " + cartItems.size() + "건을 보여드립니다.",
                    toRecommendationItemsFromCart(cartItems),
                    "cart_list",
                    null
            );
        }

        if (containsAny(message, "삭제", "빼", "제거")) {
            List<CartListItemDto> cartItems = cartService.getCartItems(email);
            if (cartItems.isEmpty()) {
                return new ChatbotAssistantResponseDto("장바구니가 비어 있어서 삭제할 항목이 없습니다.", List.of(), "cart_remove", null);
            }

            if (containsAny(message, "전부", "전체", "모두", "다")) {
                int removedCount = cartItems.size();
                cartService.removeAllCartItems(email);
                return new ChatbotAssistantResponseDto(
                        "장바구니에 담긴 숙소 " + removedCount + "건을 모두 삭제했습니다.",
                        List.of(),
                        "cart_remove",
                        null
                );
            }

            CartListItemDto target = findCartTarget(cartItems, message);
            if (target == null) {
                return new ChatbotAssistantResponseDto("삭제할 장바구니 숙소를 찾지 못했습니다. 숙소명을 함께 말씀해 주세요.", toRecommendationItemsFromCart(cartItems), "cart_remove", null);
            }

            cartService.removeCartItem(target.getCartItemId(), email);
            return new ChatbotAssistantResponseDto(
                    target.getAccomName() + "을(를) 장바구니에서 삭제했습니다.",
                    toRecommendationItemsFromCart(cartService.getCartItems(email)),
                    "cart_remove",
                    target.getAccomId()
            );
        }

        ParsedCartRequest parsed = parseCartRequest(message);
        if (parsed == null || parsed.checkInDate() == null || parsed.checkOutDate() == null) {
            return new ChatbotAssistantResponseDto("장바구니에 담으려면 지역, 숙박업소 종류, 체크인 날짜와 숙박 일수, 인원, 객실 수를 함께 알려주세요.", recommendations);
        }

        List<MainAccomDto> candidates = findCartCandidates(parsed);
        if (candidates.isEmpty()) {
            return new ChatbotAssistantResponseDto("해당 조건에 맞는 숙소가 없습니다.", List.of());
        }

        String lastFailureReason = null;
        String lastFailureAccomName = null;
        for (MainAccomDto candidate : candidates) {
            try {
                validateCartCandidate(candidate.getId(), parsed);

                CartItemDto cartItemDto = new CartItemDto();
                cartItemDto.setAccomId(candidate.getId());
                cartItemDto.setCheckInDate(parsed.checkInDate());
                cartItemDto.setCheckOutDate(parsed.checkOutDate());
                cartItemDto.setAdultCount(parsed.adultCount());
                cartItemDto.setChildCount(parsed.childCount());
                cartItemDto.setRoomCount(parsed.roomCount());
                cartService.addCartItem(cartItemDto, email);

                List<ChatbotRecommendationItemDto> resultRecommendations = recommendations.stream()
                        .sorted(Comparator.comparing((ChatbotRecommendationItemDto item) -> !candidate.getId().equals(item.getAccomId())))
                        .toList();

                String answer = "%s을(를) %s부터 %s까지 장바구니에 담았습니다. 장바구니에서 바로 확인해 보세요."
                        .formatted(
                                candidate.getAccomName(),
                                parsed.checkInDate(),
                                parsed.checkOutDate()
                        );
                return new ChatbotAssistantResponseDto(answer, resultRecommendations, "cart_added", candidate.getId());
            } catch (Exception e) {
                log.info("AI cart add skipped for accomId {}: {}", candidate.getId(), e.getMessage());
                lastFailureAccomName = candidate.getAccomName();
                lastFailureReason = normalizeCartFailureReason(e.getMessage());
            }
        }

        String reason = lastFailureReason != null && !lastFailureReason.isBlank()
                ? " 사유: " + (lastFailureAccomName != null ? lastFailureAccomName + " - " : "") + lastFailureReason
                : "";
        return new ChatbotAssistantResponseDto("조건에 맞는 숙소를 찾았지만 장바구니에 담지 못했습니다." + reason, recommendations);
    }

    private ChatbotAssistantResponseDto tryHandleWishRequest(
            String message,
            String email,
            List<ChatbotRecommendationItemDto> recommendations) {
        if (!containsAny(message, "찜", "위시")) {
            return null;
        }

        if (email == null || email.isBlank()) {
            return new ChatbotAssistantResponseDto("찜 기능은 로그인 후 사용할 수 있습니다.", recommendations);
        }

        if (containsAny(message, "보여", "조회", "목록", "확인")) {
            List<WishListDto> wishItems = wishService.getWishList(email, "priceDesc");
            return new ChatbotAssistantResponseDto(
                    wishItems.isEmpty() ? "찜 목록이 비어 있습니다." : "찜한 숙소 " + wishItems.size() + "건을 보여드립니다.",
                    toRecommendationItemsFromWish(wishItems),
                    "wish_list",
                    null
            );
        }

        List<WishListDto> wishItems = wishService.getWishList(email, "priceDesc");
        if (containsAny(message, "삭제", "해제", "취소", "빼")) {
            WishListDto target = findWishTarget(wishItems, message);
            if (target == null) {
                return new ChatbotAssistantResponseDto("삭제할 찜 숙소를 찾지 못했습니다. 숙소명을 함께 말씀해 주세요.", toRecommendationItemsFromWish(wishItems));
            }
            wishService.removeWish(target.getAccomId(), email);
            return new ChatbotAssistantResponseDto(
                    target.getAccomName() + " 찜을 해제했습니다.",
                    toRecommendationItemsFromWish(wishService.getWishList(email, "priceDesc")),
                    "wish_remove",
                    target.getAccomId()
            );
        }

        MainAccomDto target = findAccomTargetFromMessage(message, recommendations);
        if (target == null) {
            return new ChatbotAssistantResponseDto("찜할 숙소를 정확히 찾지 못했습니다. 숙소명이나 지역 조건을 더 구체적으로 말씀해 주세요.", recommendations);
        }
        wishService.addWish(target.getId(), email);
        return new ChatbotAssistantResponseDto(
                target.getAccomName() + "을(를) 찜 목록에 추가했습니다.",
                recommendations,
                "wish_added",
                target.getId()
        );
    }

    private ChatbotAssistantResponseDto tryHandleRecentViewedRequest(String message, String recentViewedCookie) {
        if (!containsAny(message, "최근 본", "최근본", "방금 본")) {
            return null;
        }

        List<Long> ids = parseRecentViewedIds(recentViewedCookie);
        List<MainAccomDto> recentItems = accomService.getRecentViewedAccomList(ids);
        return new ChatbotAssistantResponseDto(
                recentItems.isEmpty() ? "최근 본 숙소가 없습니다." : "최근 본 숙소 " + recentItems.size() + "건입니다.",
                toRecommendationItemsFromMain(recentItems),
                "recent_viewed",
                null
        );
    }

    private ChatbotAssistantResponseDto tryHandleOrderRequest(String message, String email) {
        if (!containsAny(message, "주문", "예약 내역", "예약내역", "예약 목록", "예약목록", "예약 취소", "주문 취소")) {
            return null;
        }

        if (email == null || email.isBlank()) {
            return new ChatbotAssistantResponseDto("주문과 예약 기능은 로그인 후 사용할 수 있습니다.", List.of());
        }

        if (containsAny(message, "취소")) {
            Long orderId = parseOrderId(message);
            if (orderId != null) {
                try {
                    orderService.cancelOrder(orderId, email);
                    return new ChatbotAssistantResponseDto("예약 #" + orderId + "을(를) 취소했습니다.", List.of(), "order_cancelled", null);
                } catch (Exception e) {
                    return new ChatbotAssistantResponseDto("예약 취소에 실패했습니다. " + e.getMessage(), List.of(), "order_cancelled", null);
                }
            }

            List<OrderHistDto> orders = orderService.getOrderList(email, PageRequest.of(0, 5));
            if (orders.isEmpty()) {
                return new ChatbotAssistantResponseDto("취소할 예약이 없습니다.", List.of(), "order_cancelled", null);
            }
            try {
                orderService.cancelOrder(orders.get(0).getOrderId(), email);
                return new ChatbotAssistantResponseDto("가장 최근 예약 #" + orders.get(0).getOrderId() + "을(를) 취소했습니다.", List.of(), "order_cancelled", null);
            } catch (Exception e) {
                return new ChatbotAssistantResponseDto("최근 예약 취소에 실패했습니다. " + e.getMessage(), List.of(), "order_cancelled", null);
            }
        }

        List<OrderHistDto> orders = orderService.getOrderList(email, PageRequest.of(0, 5));
        if (orders.isEmpty()) {
            return new ChatbotAssistantResponseDto("예약 내역이 없습니다.", List.of(), "order_list", null);
        }

        return new ChatbotAssistantResponseDto(
                buildOrderSummary(orders),
                toRecommendationItemsFromOrders(orders),
                "order_list",
                null
        );
    }

    private ChatbotAssistantResponseDto tryHandleNotificationRequest(String message, String email) {
        if (!containsAny(message, "알림", "공지")) {
            return null;
        }

        if (email == null || email.isBlank()) {
            return new ChatbotAssistantResponseDto("알림 조회는 로그인 후 사용할 수 있습니다.", List.of());
        }

        List<NotificationDto> notifications = notificationService.getRecentNotifications(email);
        if (notifications.isEmpty()) {
            return new ChatbotAssistantResponseDto("최근 알림이 없습니다.", List.of(), "notification_list", null);
        }

        List<String> lines = notifications.stream()
                .limit(5)
                .map(item -> "- " + item.getMessage())
                .toList();
        return new ChatbotAssistantResponseDto(
                "최근 알림입니다.\n" + String.join("\n", lines),
                List.of(),
                "notification_list",
                null
        );
    }

    private boolean isCartRequest(String message) {
        return message.contains("장바구니") && (message.contains("담아") || message.contains("넣어"));
    }

    private ParsedCartRequest parseCartRequest(String message) {
        LocalDate checkInDate = parseFlexibleCheckInDate(message);
        LocalDate checkOutDate = parseFlexibleCheckOutDate(message, checkInDate);
        Integer nights = resolveFlexibleNightCount(message, checkInDate, checkOutDate);
        if (checkInDate == null || checkOutDate == null || nights == null || nights < 1) {
            return null;
        }

        int adultCount = parseAdultCount(message);
        int childCount = parseChildCount(message);
        int roomCount = parseCount(message, "(객실|방)\\s*(\\d+)\\s*개", 2, 1);

        String locationKeyword = parseLocationKeyword(message);
        AccomType accomType = parseAccomType(message);
        int minGrade = message.contains("5성") || message.contains("5등급") ? 5
                : message.contains("4성") || message.contains("4등급") ? 4
                : accomType == AccomType.HOTEL ? 4 : 0;

        return new ParsedCartRequest(
                locationKeyword,
                accomType,
                minGrade,
                checkInDate,
                checkOutDate,
                adultCount,
                childCount,
                roomCount
        );
    }

    private ParsedCartRequest buildCartRequest(
            String locationKeyword,
            String accomTypeLabel,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int adultCount,
            int childCount,
            int roomCount) {
        AccomType accomType = resolveAccomTypeLabel(accomTypeLabel);
        int safeAdultCount = Math.max(adultCount, 1);
        int safeChildCount = Math.max(childCount, 0);
        int safeRoomCount = Math.max(roomCount, 1);
        int minGrade = accomType == AccomType.HOTEL ? 4 : 0;

        return new ParsedCartRequest(
                locationKeyword == null || locationKeyword.isBlank() ? null : locationKeyword.trim(),
                accomType,
                minGrade,
                checkInDate,
                checkOutDate,
                safeAdultCount,
                safeChildCount,
                safeRoomCount
        );
    }

    private List<MainAccomDto> findCartCandidates(ParsedCartRequest parsed) {
        List<MainAccomDto> filtered = accomService.getMainAccomPage(new AccomSearchDto(), PageRequest.of(0, 200)).getContent().stream()
                .filter(item -> parsed.locationKeyword() == null || accomService.matchesLocationKeyword(item.getLocation(), parsed.locationKeyword()))
                .filter(item -> parsed.accomType() == null || item.getAccomType() == parsed.accomType())
                .filter(item -> item.getGrade() != null && item.getGrade().getNum() >= parsed.minGrade())
                .filter(item -> item.getGuestCount() == null || item.getGuestCount() >= parsed.adultCount() + parsed.childCount())
                .filter(item -> item.getRoomCount() == null || item.getRoomCount() >= parsed.roomCount())
                .toList();

        return accomService.filterCandidatesByOperationAvailability(
                        filtered,
                        parsed.checkInDate(),
                        parsed.checkOutDate()
                ).stream()
                .sorted(Comparator
                        .comparing((MainAccomDto item) -> item.getGrade() != null ? item.getGrade().getNum() : 0, Comparator.reverseOrder())
                        .thenComparing(MainAccomDto::getAvgRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MainAccomDto::getReviewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MainAccomDto::getPricePerNight, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void validateCartCandidate(Long accomId, ParsedCartRequest parsed) {
        Accom accom = accomRepository.findWithOperationInfoById(accomId)
                .orElseThrow(() -> new IllegalArgumentException("숙소 정보를 찾을 수 없습니다."));

        GuestPricingUtils.validateGuestCount(accom.getAccomType(), parsed.adultCount(), parsed.childCount());
        orderService.validateBooking(
                accom,
                parsed.checkInDate(),
                parsed.checkOutDate(),
                parsed.adultCount() + parsed.childCount(),
                null,
                parsed.roomCount()
        );
    }

    private String normalizeCartFailureReason(String message) {
        if (message == null || message.isBlank()) {
            return "예약 가능 여부를 확인하지 못했습니다.";
        }
        if (message.contains("예약 불가") || message.contains("예약이 불가") || message.contains("현재 예약")) {
            return "현재 예약 가능한 상태가 아닙니다.";
        }
        if (message.contains("운영하지 않는 날짜")) {
            return "선택한 날짜 중 운영하지 않는 날짜가 포함되어 있습니다.";
        }
        if (message.contains("운영 시작일 이전")) {
            return "숙소 운영 시작일 이전 날짜는 예약할 수 없습니다.";
        }
        if (message.contains("운영 종료일 이후")) {
            return "숙소 운영 종료일 이후 날짜는 예약할 수 없습니다.";
        }
        if (message.contains("예약이 마감된 날짜")) {
            return "선택한 날짜에 예약 가능한 객실이 없습니다.";
        }
        if (message.contains("최대 수용 인원")) {
            return message;
        }
        if (message.contains("객실 수")) {
            return message;
        }
        return message;
    }

    private LocalDate parseCheckInDate(String message) {
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일").matcher(message);
        if (!matcher.find()) {
            return null;
        }

        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        LocalDate today = LocalDate.now();

        try {
            LocalDate parsed = LocalDate.of(today.getYear(), month, day);
            if (parsed.isBefore(today)) {
                parsed = parsed.plusYears(1);
            }
            return parsed;
        } catch (java.time.DateTimeException e) {
            return null;
        }
    }

    private Integer parseNights(String message) {
        Matcher nightsMatcher = Pattern.compile("(\\d+)\\s*박").matcher(message);
        if (nightsMatcher.find()) {
            return Integer.parseInt(nightsMatcher.group(1));
        }
        return null;
    }

    private LocalDate parseFlexibleCheckInDate(String message) {
        LocalDate labeledIsoDate = parseLabeledIsoDate(message, "체크인");
        if (labeledIsoDate != null) {
            return labeledIsoDate;
        }

        LocalDate firstIsoDate = parseIsoDateByIndex(message, 0);
        if (firstIsoDate != null) {
            return firstIsoDate;
        }

        LocalDate firstMonthDayDate = parseMonthDayDateByIndex(message, 0, null);
        if (firstMonthDayDate != null) {
            return firstMonthDayDate;
        }

        return parseCheckInDate(message);
    }

    private LocalDate parseFlexibleCheckOutDate(String message, LocalDate checkInDate) {
        LocalDate labeledIsoDate = parseLabeledIsoDate(message, "체크아웃");
        if (labeledIsoDate != null) {
            return labeledIsoDate;
        }

        LocalDate secondIsoDate = parseIsoDateByIndex(message, 1);
        if (secondIsoDate != null) {
            return secondIsoDate;
        }

        LocalDate secondMonthDayDate = parseMonthDayDateByIndex(message, 1, checkInDate);
        if (secondMonthDayDate != null) {
            return secondMonthDayDate;
        }

        LocalDate untilMonthDayDate = parseUntilMonthDayDate(message, checkInDate);
        if (untilMonthDayDate != null) {
            return untilMonthDayDate;
        }

        Integer nights = parseFlexibleNights(message);
        if (checkInDate != null && nights != null && nights > 0) {
            return checkInDate.plusDays(nights);
        }

        return null;
    }

    private Integer resolveFlexibleNightCount(String message, LocalDate checkInDate, LocalDate checkOutDate) {
        Integer explicitNights = parseFlexibleNights(message);
        if (explicitNights != null) {
            return explicitNights;
        }
        if (checkInDate != null && checkOutDate != null && checkOutDate.isAfter(checkInDate)) {
            return (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        }
        return null;
    }

    private Integer parseFlexibleNights(String message) {
        Matcher nightsMatcher = Pattern.compile("(\\d+)\\s*박").matcher(message);
        if (nightsMatcher.find()) {
            return Integer.parseInt(nightsMatcher.group(1));
        }
        return parseNights(message);
    }

    private LocalDate parseLabeledIsoDate(String message, String label) {
        Matcher matcher = Pattern.compile(label + "\\s*(\\d{4}-\\d{2}-\\d{2})").matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return parseIsoDate(matcher.group(1));
    }

    private LocalDate parseIsoDateByIndex(String message, int index) {
        Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(message);
        int currentIndex = 0;
        while (matcher.find()) {
            if (currentIndex == index) {
                return parseIsoDate(matcher.group(1));
            }
            currentIndex += 1;
        }
        return null;
    }

    private LocalDate parseIsoDate(String rawDate) {
        try {
            return LocalDate.parse(rawDate);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseMonthDayDateByIndex(String message, int index, LocalDate baseDate) {
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일").matcher(message);
        int currentIndex = 0;
        while (matcher.find()) {
            if (currentIndex == index) {
                int month = Integer.parseInt(matcher.group(1));
                int day = Integer.parseInt(matcher.group(2));
                return resolveMonthDayDate(month, day, baseDate);
            }
            currentIndex += 1;
        }
        return null;
    }

    private LocalDate parseUntilMonthDayDate(String message, LocalDate checkInDate) {
        if (checkInDate == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("(?:(\\d{1,2})\\s*월\\s*)?(\\d{1,2})\\s*일?\\s*까지").matcher(message);
        LocalDate parsed = null;
        while (matcher.find()) {
            int month = matcher.group(1) != null
                    ? Integer.parseInt(matcher.group(1))
                    : checkInDate.getMonthValue();
            int day = Integer.parseInt(matcher.group(2));
            parsed = resolveMonthDayDate(month, day, checkInDate);
        }
        return parsed;
    }

    private LocalDate resolveMonthDayDate(int month, int day, LocalDate baseDate) {
        LocalDate anchorDate = baseDate != null ? baseDate : LocalDate.now();
        try {
            LocalDate parsed = LocalDate.of(anchorDate.getYear(), month, day);
            if (baseDate != null) {
                return parsed.isBefore(baseDate) ? parsed.plusYears(1) : parsed;
            }
            return parsed.isBefore(anchorDate) ? parsed.plusYears(1) : parsed;
        } catch (Exception e) {
            return null;
        }
    }

    private String parseLocationKeyword(String message) {
        return accomService.extractLocationKeyword(message).orElse(null);
    }

    private AccomType parseAccomType(String message) {
        if (message.contains("호텔")) {
            return AccomType.HOTEL;
        }
        if (message.contains("리조트")) {
            return AccomType.RESORT;
        }
        if (message.contains("펜션")) {
            return AccomType.PENSION;
        }
        if (message.contains("모텔")) {
            return AccomType.MOTEL;
        }
        if (message.contains("게스트하우스")) {
            return AccomType.GUESTHOUSE;
        }
        return null;
    }

    private AccomType resolveAccomTypeLabel(String accomTypeLabel) {
        if (accomTypeLabel == null || accomTypeLabel.isBlank() || "전체".equals(accomTypeLabel)) {
            return null;
        }
        return parseAccomType(accomTypeLabel);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean containsAny(String message, String... keywords) {
        return Arrays.stream(keywords).anyMatch(message::contains);
    }

    private String extractReservationTargetName(String message) {
        String cleaned = message == null ? "" : message;
        cleaned = cleaned.replace("예약해줘", "")
                .replace("예약 해줘", "")
                .replace("예약해주세요", "")
                .replace("예약 부탁해", "")
                .replace("예약", "")
                .trim();
        return cleaned.isBlank() ? "입력한 업소명" : cleaned;
    }

    private boolean containsNormalized(String source, String keyword) {
        if (source == null || keyword == null || keyword.isBlank()) {
            return false;
        }
        return normalizeForMatch(source).contains(normalizeForMatch(keyword));
    }

    private String normalizeForMatch(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private int parseCount(String message, String regex, int groupIndex, int defaultValue) {
        Matcher matcher = Pattern.compile(regex).matcher(message);
        if (!matcher.find()) {
            return defaultValue;
        }
        return Integer.parseInt(matcher.group(groupIndex));
    }

    private int parseAdultCount(String message) {
        Integer explicitAdultCount = parseOptionalCount(message, "(성인|어른)\\s*(\\d+)\\s*(명|인)?", 2);
        if (explicitAdultCount != null) {
            return explicitAdultCount;
        }

        Integer totalGuestCount = parseOptionalCount(message, "(\\d+)\\s*(명|인)", 1);
        if (totalGuestCount != null) {
            Integer childCount = parseOptionalCount(message, "(아동|아이|어린이)\\s*(\\d+)\\s*(명|인)?", 2);
            return Math.max(1, totalGuestCount - (childCount != null ? childCount : 0));
        }

        return 1;
    }

    private int parseChildCount(String message) {
        Integer childCount = parseOptionalCount(message, "(아동|아이|어린이)\\s*(\\d+)\\s*(명|인)?", 2);
        return childCount != null ? childCount : 0;
    }

    private Integer parseOptionalCount(String message, String regex, int groupIndex) {
        Matcher matcher = Pattern.compile(regex).matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(groupIndex));
    }

    private Long parseOrderId(String message) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(번|호)?").matcher(message);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private List<Long> parseRecentViewedIds(String recentViewedCookie) {
        if (recentViewedCookie == null || recentViewedCookie.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recentViewedCookie.split("[,-]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .distinct()
                .limit(8)
                .toList();
    }

    private CartListItemDto findCartTarget(List<CartListItemDto> items, String message) {
        for (CartListItemDto item : items) {
            if (containsNormalized(message, item.getAccomName())) {
                return item;
            }
        }
        return items.isEmpty() ? null : items.get(0);
    }

    private WishListDto findWishTarget(List<WishListDto> items, String message) {
        for (WishListDto item : items) {
            if (containsNormalized(message, item.getAccomName())) {
                return item;
            }
        }
        return items.isEmpty() ? null : items.get(0);
    }

    private MainAccomDto findAccomTargetFromMessage(String message, List<ChatbotRecommendationItemDto> recommendations) {
        for (ChatbotRecommendationItemDto item : recommendations) {
            if (containsNormalized(message, item.getAccomName())) {
                return accomService.getMainAccomPage(new AccomSearchDto(), PageRequest.of(0, 200)).getContent().stream()
                        .filter(accom -> item.getAccomId().equals(accom.getId()))
                        .findFirst()
                        .orElse(null);
            }
        }
        ParsedCartRequest parsed = parseCartRequestWithOptionalDates(message);
        List<MainAccomDto> candidates = findCartCandidates(parsed);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private ParsedCartRequest parseCartRequestWithOptionalDates(String message) {
        LocalDate checkInDate = parseFlexibleCheckInDate(message);
        LocalDate checkOutDate = parseFlexibleCheckOutDate(message, checkInDate);
        String locationKeyword = parseLocationKeyword(message);
        AccomType accomType = parseAccomType(message);
        int minGrade = message.contains("5성") || message.contains("5등급") ? 5
                : message.contains("4성") || message.contains("4등급") ? 4
                : accomType == AccomType.HOTEL ? 4 : 0;

        return new ParsedCartRequest(
                locationKeyword,
                accomType,
                minGrade,
                checkInDate,
                checkOutDate,
                parseAdultCount(message),
                parseChildCount(message),
                parseCount(message, "(객실|방)\\s*(\\d+)\\s*개", 2, 1)
        );
    }

    private List<ChatbotRecommendationItemDto> toRecommendationItemsFromMain(List<MainAccomDto> items) {
        return items.stream()
                .map(this::toRecommendationItem)
                .toList();
    }

    private List<ChatbotRecommendationItemDto> toRecommendationItemsFromWish(List<WishListDto> items) {
        return items.stream()
                .map(item -> new ChatbotRecommendationItemDto(
                        item.getAccomId(),
                        item.getAccomName(),
                        item.getAccomType() != null ? item.getAccomType().getLabel() : "숙소",
                        item.getGrade() != null ? item.getGrade().getNum() : 0,
                        item.getLocation(),
                        item.getPricePerNight(),
                        item.getAvgRating(),
                        item.getReviewCount(),
                        item.getImgUrl(),
                        List.of("찜 목록에 저장된 숙소입니다.")
                ))
                .toList();
    }

    private List<ChatbotRecommendationItemDto> toRecommendationItemsFromCart(List<CartListItemDto> items) {
        return items.stream()
                .map(item -> new ChatbotRecommendationItemDto(
                        item.getAccomId(),
                        item.getAccomName(),
                        item.getAccomTypeName(),
                        0,
                        item.getCheckInDate() + " ~ " + item.getCheckOutDate(),
                        item.getTotalPrice(),
                        null,
                        null,
                        item.getRepImgUrl(),
                        List.of(item.isReservable() ? "장바구니에 담긴 예약 대기 항목입니다." : item.getUnavailableReason())
                ))
                .toList();
    }

    private List<ChatbotRecommendationItemDto> toRecommendationItemsFromOrders(List<OrderHistDto> orders) {
        List<ChatbotRecommendationItemDto> items = new ArrayList<>();
        for (OrderHistDto order : orders) {
            for (OrderItemDto item : order.getOrderItemDtos()) {
                items.add(new ChatbotRecommendationItemDto(
                        null,
                        item.getAccomName(),
                        item.getGradeLabel(),
                        0,
                        item.getCheckInDate() + " ~ " + item.getCheckOutDate(),
                        item.getTotalPrice(),
                        null,
                        null,
                        item.getImgUrl(),
                        List.of("예약 #" + order.getOrderId() + " / 상태: " + order.getOrderStatus())
                ));
            }
        }
        return items;
    }

    private ChatbotRecommendationItemDto toRecommendationItem(MainAccomDto item) {
        return new ChatbotRecommendationItemDto(
                item.getId(),
                item.getAccomName(),
                item.getAccomType() != null ? item.getAccomType().getLabel() : "숙소",
                item.getGrade() != null ? item.getGrade().getNum() : 0,
                item.getLocation(),
                item.getPricePerNight(),
                item.getAvgRating(),
                item.getReviewCount(),
                item.getImgUrl(),
                List.of("최근 조회 또는 액션 결과로 불러온 숙소입니다.")
        );
    }

    private String buildOrderSummary(List<OrderHistDto> orders) {
        List<String> lines = new ArrayList<>();
        for (OrderHistDto order : orders.stream().limit(5).toList()) {
            lines.add("- 예약 #" + order.getOrderId() + " / 상태: " + order.getOrderStatus() + " / 총액: " + String.format(Locale.KOREA, "%,d원", order.getTotalPrice()));
        }
        return "최근 예약 내역입니다.\n" + String.join("\n", lines);
    }

    private String buildRecommendationContext(List<ChatbotRecommendationItemDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "추천 후보 없음";
        }

        List<String> lines = new ArrayList<>();
        for (ChatbotRecommendationItemDto item : recommendations) {
            lines.add("- %s | 위치: %s | 유형: %s | 가격: %s | 평점: %s | 추천 이유: %s"
                    .formatted(
                            nullSafe(item.getAccomName()),
                            nullSafe(item.getLocation()),
                            nullSafe(item.getAccomType()),
                            item.getPricePerNight() != null ? String.format(Locale.KOREA, "%,d원", item.getPricePerNight()) : "-",
                            item.getAvgRating() != null ? String.format(Locale.KOREA, "%.1f", item.getAvgRating()) : "-",
                            joinReasons(item.getReasons())
                    ));
        }
        return String.join("\n", lines);
    }

    private String extractMessageContent(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode choices = response.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return null;
        }

        JsonNode message = choices.get(0).get("message");
        if (message == null) {
            return null;
        }

        JsonNode content = message.get("content");
        if (content == null || content.isNull()) {
            return null;
        }

        if (content.isTextual()) {
            return content.asText();
        }

        if (content.isArray()) {
            List<String> texts = new ArrayList<>();
            for (JsonNode item : content) {
                JsonNode type = item.get("type");
                JsonNode text = item.get("text");
                if (type != null && "text".equals(type.asText()) && text != null && text.isTextual()) {
                    texts.add(text.asText());
                }
            }
            return texts.isEmpty() ? null : String.join("\n", texts);
        }

        return null;
    }

    private AiAssistantPayload parsePayload(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(extractJsonObject(rawText), AiAssistantPayload.class);
        } catch (Exception e) {
            log.warn("Failed to parse assistant payload: {}", rawText);
            return null;
        }
    }

    private String resolveAnswer(
            AiAssistantPayload payload,
            String rawText,
            List<ChatbotRecommendationItemDto> recommendations) {
        if (payload != null && payload.answer() != null && !payload.answer().isBlank()) {
            return payload.answer().trim();
        }

        if (rawText != null && !rawText.isBlank()) {
            String cleaned = stripMarkdownFence(rawText).trim();
            if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
                return cleaned;
            }
        }

        return buildFallbackAnswer(recommendations);
    }

    private String extractJsonObject(String rawText) {
        String cleaned = stripMarkdownFence(rawText).trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String stripMarkdownFence(String rawText) {
        String cleaned = rawText == null ? "" : rawText.trim();
        if (cleaned.startsWith("```")) {
            int firstNewLine = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewLine >= 0 && lastFence > firstNewLine) {
                return cleaned.substring(firstNewLine + 1, lastFence);
            }
        }
        return cleaned;
    }

    private List<ChatbotRecommendationItemDto> applyAiReasons(
            List<ChatbotRecommendationItemDto> recommendations,
            List<AiRecommendationPayload> rewrittenRecommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return recommendations;
        }

        if (rewrittenRecommendations == null || rewrittenRecommendations.isEmpty()) {
            return recommendations;
        }

        Map<Long, List<String>> reasonMap = rewrittenRecommendations.stream()
                .filter(item -> item != null && item.accomId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        AiRecommendationPayload::accomId,
                        item -> normalizeReasons(item.reasons()),
                        (left, right) -> right
                ));

        List<ChatbotRecommendationItemDto> updated = new ArrayList<>();
        for (ChatbotRecommendationItemDto item : recommendations) {
            List<String> rewritten = reasonMap.get(item.getAccomId());
            updated.add(new ChatbotRecommendationItemDto(
                    item.getAccomId(),
                    item.getAccomName(),
                    item.getAccomType(),
                    item.getGrade(),
                    item.getLocation(),
                    item.getPricePerNight(),
                    item.getAvgRating(),
                    item.getReviewCount(),
                    item.getImgUrl(),
                    rewritten == null || rewritten.isEmpty() ? item.getReasons() : rewritten
            ));
        }

        return updated;
    }

    private List<String> normalizeReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return List.of();
        }

        return reasons.stream()
                .filter(reason -> reason != null && !reason.isBlank())
                .map(String::trim)
                .limit(3)
                .toList();
    }

    private String buildFallbackAnswer(List<ChatbotRecommendationItemDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "현재는 추천 가능한 숙소 데이터가 부족합니다. 지역, 날짜, 예산을 조금 더 구체적으로 입력해 주세요.";
        }

        List<String> names = recommendations.stream()
                .limit(3)
                .map(ChatbotRecommendationItemDto::getAccomName)
                .toList();

        return "상세보기를 클릭하면 자세한 정보를 확인할 수 있습니다.";
    }

    private String joinReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "-";
        }
        return String.join(", ", reasons);
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void emit(SseEmitter emitter, String eventName, String data) throws Exception {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data == null ? "" : data));
    }

    private List<String> splitAnswer(String answer) {
        String safeAnswer = answer == null ? "" : answer.trim();
        if (safeAnswer.isBlank()) {
            return List.of("응답을 생성하지 못했습니다.");
        }

        String normalized = safeAnswer
                .replace(". ", ".\n")
                .replace("! ", "!\n")
                .replace("? ", "?\n");

        List<String> chunks = normalized.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        return chunks.isEmpty() ? List.of(safeAnswer) : chunks;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(60L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record AiAssistantPayload(String answer, List<AiRecommendationPayload> recommendations) {
    }

    private record AiRecommendationPayload(Long accomId, List<String> reasons) {
    }

    private record ParsedCartRequest(
            String locationKeyword,
            AccomType accomType,
            int minGrade,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int adultCount,
            int childCount,
            int roomCount) {
    }
}
