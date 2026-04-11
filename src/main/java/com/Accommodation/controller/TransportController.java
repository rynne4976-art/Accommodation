package com.Accommodation.controller;

import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.service.AccomService;
import com.Accommodation.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class TransportController {

    private static final int TRANSPORT_RADIUS = 800;
    private static final String USER_AGENT = "Accommodation/1.0 transport";
    private static final List<String> OVERPASS_ENDPOINTS = List.of(
            "https://overpass-api.de/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
    );

    private static final List<String> SESSION_USER_KEYS = List.of(
            "loginMember",
            "member",
            "loginUser",
            "user",
            "sessionUser",
            "authenticatedUser",
            "principal",
            "SPRING_SECURITY_CONTEXT"
    );

    private static final List<String> ADDRESS_METHOD_NAMES = List.of(
            "getAddress",
            "getAddr",
            "getRoadAddress",
            "getMemberAddr",
            "getUserAddress",
            "getAddress1",
            "getAddress2"
    );

    private static final List<String> ADDRESS_FIELD_NAMES = List.of(
            "address",
            "addr",
            "roadAddress",
            "memberAddr",
            "userAddress",
            "address1",
            "address2"
    );

    private static final List<String> NESTED_METHOD_NAMES = List.of(
            "getMember",
            "getUser",
            "getPrincipal",
            "getDetails",
            "getAccount"
    );

    private static final List<String> NESTED_FIELD_NAMES = List.of(
            "member",
            "user",
            "principal",
            "details",
            "account"
    );

    private final AccomService accomService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Value("${odsay.api.key:}")
    private String odsayApiKey;

    @Value("${odsay.api.web-key:}")
    private String odsayWebKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final class TransitCandidate {
        private final String type;
        private final String title;
        private final int totalTime;
        private final int payment;
        private final int busTransitCount;
        private final int subwayTransitCount;
        private final int totalWalk;
        private final double distanceKm;
        private final int pathType;
        private final String detail;
        private final String firstStartStation;
        private final String lastEndStation;
        private final List<Map<String, Object>> steps;

        private TransitCandidate(
                String type,
                String title,
                int totalTime,
                int payment,
                int busTransitCount,
                int subwayTransitCount,
                int totalWalk,
                double distanceKm,
                int pathType,
                String detail,
                String firstStartStation,
                String lastEndStation,
                List<Map<String, Object>> steps
        ) {
            this.type = type;
            this.title = title;
            this.totalTime = totalTime;
            this.payment = payment;
            this.busTransitCount = busTransitCount;
            this.subwayTransitCount = subwayTransitCount;
            this.totalWalk = totalWalk;
            this.distanceKm = distanceKm;
            this.pathType = pathType;
            this.detail = detail;
            this.firstStartStation = firstStartStation;
            this.lastEndStation = lastEndStation;
            this.steps = steps;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", type);
            data.put("title", title);
            data.put("totalTime", totalTime);
            data.put("minutes", totalTime);
            data.put("payment", payment);
            data.put("paymentAmount", payment);
            data.put("busTransitCount", busTransitCount);
            data.put("subwayTransitCount", subwayTransitCount);
            data.put("totalWalk", totalWalk);
            data.put("distanceKm", distanceKm);
            data.put("pathType", pathType);
            data.put("detail", detail);
            data.put("firstStartStation", firstStartStation);
            data.put("lastEndStation", lastEndStation);
            data.put("steps", steps);
            return data;
        }
    }

    @GetMapping("/transport")
    public String transportPage(
            @RequestParam(value = "accomId", required = false) Long accomId,
            @RequestParam(value = "myAddress", required = false) String myAddress,
            HttpSession session,
            Model model
    ) {
        Accom accom = accomId != null ? accomService.getAccomDtl(accomId) : null;
        String resolvedMyPageAddress = resolveMyPageAddress(myAddress, session);

        model.addAttribute("accom", accom);
        model.addAttribute("selectableAccomList", accomService.getTransportSelectableAccomList());
        model.addAttribute("odsayWebKey", odsayWebKey);
        model.addAttribute("myPageAddress", resolvedMyPageAddress);

        return "transport/transport";
    }

    @GetMapping(value = "/api/transport/accommodation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> accommodation(@RequestParam("id") Long accomId) {
        try {
            Accom accom = accomService.getAccomDtl(accomId);
            return ResponseEntity.ok(Map.of(
                    "id", accom.getId(),
                    "name", safeText(accom.getAccomName(), "숙소"),
                    "location", safeText(accom.getLocation(), ""),
                    "imageUrl", resolvePrimaryImage(accom),
                    "detail", safeText(accom.getAccomDetail(), "숙소 설명이 없습니다."),
                    "pricePerNight", accom.getPricePerNight() != null ? accom.getPricePerNight() : 0,
                    "grade", accom.getGrade() != null ? accom.getGrade().getNum() : 0,
                    "avgRating", accom.getAvgRating() != null ? accom.getAvgRating() : 0.0,
                    "reviewCount", accom.getReviewCount() != null ? accom.getReviewCount() : 0
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "숙소 정보를 찾지 못했습니다."));
        }
    }

    @GetMapping(value = "/api/transport/my-address", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> myAddress(HttpSession session) {
        String address = resolveMyPageAddress(null, session);
        if (!isMeaningfulAddress(address)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "마이페이지에 등록된 주소가 없습니다."));
        }
        return ResponseEntity.ok(Map.of("address", address));
    }

    @GetMapping(value = "/api/transport/reserved-accommodation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> reservedAccommodation(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        List<Map<String, Object>> items = orderService.getReservedAccommodations(principal.getName(), 20).stream()
                .map(accom -> Map.<String, Object>of(
                        "id", accom.getId(),
                        "name", safeText(accom.getAccomName(), "숙소"),
                        "location", safeText(accom.getLocation(), ""),
                        "imageUrl", resolvePrimaryImage(accom)
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "count", items.size(),
                "items", items
        ));
    }

    @GetMapping(value = "/api/transport/accommodation-search", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> searchAccommodations(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "minPrice", required = false) Integer minPrice,
            @RequestParam(value = "maxPrice", required = false) Integer maxPrice,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        int safePage = Math.max(page, 0);

        com.Accommodation.dto.AccomSearchDto searchDto = new com.Accommodation.dto.AccomSearchDto();
        searchDto.setSearchQuery(keyword);
        searchDto.setMinPrice(minPrice);
        searchDto.setMaxPrice(maxPrice);
        searchDto.setMinRating(minRating);
        searchDto.setGrade(parseGrade(grade));

        Page<com.Accommodation.dto.MainAccomDto> accomPage =
                accomService.getMainAccomPage(searchDto, PageRequest.of(safePage, 5));

        List<Map<String, Object>> items = accomPage.getContent().stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.getId(),
                        "name", safeText(item.getAccomName(), "숙소"),
                        "location", safeText(item.getLocation(), ""),
                        "imageUrl", safeText(item.getImgUrl(), ""),
                        "grade", item.getGrade() != null ? item.getGrade().getNum() : 0,
                        "pricePerNight", item.getPricePerNight() != null ? item.getPricePerNight() : 0,
                        "avgRating", item.getAvgRating() != null ? item.getAvgRating() : 0.0,
                        "reviewCount", item.getReviewCount() != null ? item.getReviewCount() : 0
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", accomPage.getNumber(),
                "size", accomPage.getSize(),
                "totalPages", accomPage.getTotalPages(),
                "totalElements", accomPage.getTotalElements()
        ));
    }

    private String resolveMyPageAddress(String requestAddress, HttpSession session) {
        if (isMeaningfulAddress(requestAddress)) {
            return requestAddress.trim();
        }

        if (session == null) {
            return "";
        }

        for (String sessionKey : SESSION_USER_KEYS) {
            Object sessionValue = session.getAttribute(sessionKey);
            String extracted = extractAddressDeep(sessionValue, new IdentityHashMap<>());
            if (isMeaningfulAddress(extracted)) {
                return extracted.trim();
            }
        }

        return "";
    }

    private com.Accommodation.constant.AccomGrade parseGrade(String grade) {
        if (grade == null || grade.isBlank()) {
            return null;
        }

        String normalized = grade.trim().toUpperCase();
        try {
            return com.Accommodation.constant.AccomGrade.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
        }

        return switch (normalized) {
            case "1", "ONE" -> com.Accommodation.constant.AccomGrade.ONE;
            case "2", "TWO" -> com.Accommodation.constant.AccomGrade.TWO;
            case "3", "THREE" -> com.Accommodation.constant.AccomGrade.THREE;
            case "4", "FOUR" -> com.Accommodation.constant.AccomGrade.FOUR;
            case "5", "FIVE" -> com.Accommodation.constant.AccomGrade.FIVE;
            default -> null;
        };
    }

    private String extractAddressDeep(Object target, IdentityHashMap<Object, Boolean> visited) {
        if (target == null) {
            return "";
        }

        if (visited.containsKey(target)) {
            return "";
        }
        visited.put(target, Boolean.TRUE);

        if (target instanceof String str) {
            return isMeaningfulAddress(str) ? str.trim() : "";
        }

        if (target instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                String extracted = extractAddressDeep(value, visited);
                if (isMeaningfulAddress(extracted)) {
                    return extracted;
                }
            }
        }

        if (target instanceof Iterable<?> iterable) {
            for (Object value : iterable) {
                String extracted = extractAddressDeep(value, visited);
                if (isMeaningfulAddress(extracted)) {
                    return extracted;
                }
            }
        }

        String directAddress = extractAddressFromObject(target);
        if (isMeaningfulAddress(directAddress)) {
            return directAddress;
        }

        for (String methodName : NESTED_METHOD_NAMES) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object nested = method.invoke(target);
                String extracted = extractAddressDeep(nested, visited);
                if (isMeaningfulAddress(extracted)) {
                    return extracted;
                }
            } catch (Exception ignored) {
            }
        }

        for (String fieldName : NESTED_FIELD_NAMES) {
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object nested = field.get(target);
                String extracted = extractAddressDeep(nested, visited);
                if (isMeaningfulAddress(extracted)) {
                    return extracted;
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private String extractAddressFromObject(Object target) {
        if (target == null) {
            return "";
        }

        for (String methodName : ADDRESS_METHOD_NAMES) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof String str && isMeaningfulAddress(str)) {
                    return str.trim();
                }
            } catch (Exception ignored) {
            }
        }

        for (String fieldName : ADDRESS_FIELD_NAMES) {
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof String str && isMeaningfulAddress(str)) {
                    return str.trim();
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private boolean isMeaningfulAddress(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            return false;
        }

        String normalized = text.replace(" ", "");
        if (normalized.equalsIgnoreCase("null")
                || normalized.equals("주소")
                || normalized.equals("도로명주소")
                || normalized.equals("지번주소")
                || normalized.contains("입력된정보없음")
                || normalized.length() < 5) {
            return false;
        }

        return text.contains("로")
                || text.contains("길")
                || text.contains("동")
                || text.contains("읍")
                || text.contains("면")
                || text.contains("리")
                || text.contains("구")
                || text.contains("군")
                || text.contains("시")
                || text.contains("도")
                || text.contains("로")
                || text.matches(".*\\d{2,}.*");
    }

    @GetMapping(value = "/api/transport/geocode", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> geocode(@RequestParam("address") String address) {
        if (address == null || address.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "주소가 필요합니다."));
        }

        try {
            JsonNode locations = requestJson(
                    "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
                            + URLEncoder.encode(address, StandardCharsets.UTF_8)
            );

            if (!locations.isArray() || locations.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "주소 좌표를 찾지 못했습니다."));
            }

            JsonNode first = locations.get(0);
            return ResponseEntity.ok(Map.of(
                    "lat", first.path("lat").asDouble(),
                    "lng", first.path("lon").asDouble(),
                    "address", first.path("display_name").asText(address)
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "주소 조회에 실패했습니다."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "주소 조회에 실패했습니다."));
        }
    }

    @GetMapping(value = "/api/transport/nearby", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> nearbyTransport(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng
    ) {
        String query = """
                [out:json][timeout:20];
                (
                  node["railway"="station"](around:%d,%s,%s);
                  node["highway"="bus_stop"](around:%d,%s,%s);
                );
                out body;
                """.formatted(
                TRANSPORT_RADIUS, lat, lng,
                TRANSPORT_RADIUS, lat, lng
        );

        try {
            JsonNode response = requestJsonWithFallback(
                    OVERPASS_ENDPOINTS,
                    query,
                    MediaType.TEXT_PLAIN_VALUE
            );

            List<Map<String, Object>> items = mapTransportItems(response.path("elements"), lat, lng);
            return ResponseEntity.ok(Map.of("items", items));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of(
                    "items", List.of(),
                    "message", "주변 교통 정보를 불러오지 못했습니다."
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.ok(Map.of(
                    "items", List.of(),
                    "message", "주변 교통 정보를 불러오지 못했습니다."
            ));
        }
    }

    @GetMapping(value = "/api/transport/road-route", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> roadRoute(
            @RequestParam("profile") String profile,
            @RequestParam("sx") double startLng,
            @RequestParam("sy") double startLat,
            @RequestParam("ex") double endLng,
            @RequestParam("ey") double endLat
    ) {
        String normalizedProfile = "foot".equalsIgnoreCase(profile) ? "foot" : "driving";
        String baseUrl = "foot".equals(normalizedProfile)
                ? "https://routing.openstreetmap.de/routed-foot/route/v1/foot"
                : "https://routing.openstreetmap.de/routed-car/route/v1/driving";

        String url = baseUrl
                + "/" + startLng + "," + startLat + ";" + endLng + "," + endLat
                + "?overview=full&geometries=geojson";

        try {
            JsonNode response = requestJson(url);
            JsonNode routes = response.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "경로를 찾지 못했습니다."));
            }
            return ResponseEntity.ok(routes.get(0));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "경로를 불러오지 못했습니다."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "경로를 불러오지 못했습니다."));
        }
    }

    @GetMapping(value = "/api/transport/transit-route", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> transitRoute(
            @RequestParam("sx") double startLng,
            @RequestParam("sy") double startLat,
            @RequestParam("ex") double endLng,
            @RequestParam("ey") double endLat
    ) {
        if (odsayApiKey == null || odsayApiKey.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "대중교통 API 키가 설정되지 않았습니다.",
                    "alternatives", List.of()
            ));
        }

        try {
            List<TransitCandidate> candidates = loadTransitCandidates(startLng, startLat, endLng, endLat);
            if (candidates.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "available", false,
                        "message", "대중교통 경로를 찾지 못했습니다.",
                        "alternatives", List.of()
                ));
            }

            TransitCandidate best = candidates.get(0);
            Map<String, Object> payload = new LinkedHashMap<>(best.toMap());
            payload.put("available", true);
            payload.put("alternatives", buildAlternativeMaps(candidates));
            return ResponseEntity.ok(payload);
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "대중교통 경로를 불러오지 못했습니다.",
                    "alternatives", List.of()
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "대중교통 경로를 불러오지 못했습니다.",
                    "alternatives", List.of()
            ));
        }
    }

    @GetMapping(value = "/api/transport/intercity-recommend", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> intercityRecommend(
            @RequestParam("sx") double startLng,
            @RequestParam("sy") double startLat,
            @RequestParam("ex") double endLng,
            @RequestParam("ey") double endLat
    ) {
        if (odsayApiKey == null || odsayApiKey.isBlank()) {
            return ResponseEntity.ok(Map.of("items", List.of()));
        }

        try {
            List<Map<String, Object>> items = buildAlternativeMaps(
                    loadTransitCandidates(startLng, startLat, endLng, endLat).stream()
                            .filter(candidate -> Set.of("train", "express", "intercity").contains(candidate.type))
                            .toList()
            );
            return ResponseEntity.ok(Map.of("items", items));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of(
                    "items", List.of(),
                    "message", "장거리 대중교통 추천 정보를 불러오지 못했습니다."
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.ok(Map.of(
                    "items", List.of(),
                    "message", "장거리 대중교통 추천 정보를 불러오지 못했습니다."
            ));
        }
    }

    private List<TransitCandidate> loadTransitCandidates(
            double startLng,
            double startLat,
            double endLng,
            double endLat
    ) throws IOException, InterruptedException {
        List<String> urls = List.of(
                buildTransitSearchUrl("searchPubTransPathT", startLng, startLat, endLng, endLat, null),
                buildTransitSearchUrl("searchPubTransPathR", startLng, startLat, endLng, endLat, "0"),
                buildTransitSearchUrl("searchPubTransPathR", startLng, startLat, endLng, endLat, "1")
        );

        List<TransitCandidate> candidates = new ArrayList<>();
        for (String url : urls) {
            try {
                JsonNode response = requestJson(url);
                if (hasOdsayError(response)) {
                    continue;
                }

                JsonNode paths = response.path("result").path("path");
                if (!paths.isArray()) {
                    continue;
                }

                for (JsonNode path : paths) {
                    TransitCandidate candidate = toTransitCandidate(path);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            } catch (IOException e) {
                // Try the next API variant.
            }
        }

        return dedupeCandidates(candidates);
    }

    private String buildTransitSearchUrl(
            String apiName,
            double startLng,
            double startLat,
            double endLng,
            double endLat,
            String searchType
    ) {
        StringBuilder url = new StringBuilder("https://api.odsay.com/v1/api/")
                .append(apiName)
                .append("?apiKey=").append(URLEncoder.encode(odsayApiKey, StandardCharsets.UTF_8))
                .append("&SX=").append(startLng)
                .append("&SY=").append(startLat)
                .append("&EX=").append(endLng)
                .append("&EY=").append(endLat)
                .append("&OPT=0&lang=0");

        if (searchType != null) {
            url.append("&SearchType=").append(searchType);
        }

        return url.toString();
    }

    private List<TransitCandidate> dedupeCandidates(List<TransitCandidate> candidates) {
        Map<String, TransitCandidate> unique = new LinkedHashMap<>();
        candidates.stream()
                .sorted(Comparator
                        .comparingInt((TransitCandidate item) -> item.totalTime)
                        .thenComparingInt(item -> transportTypePriority(item.type))
                        .thenComparing(item -> item.title))
                .forEach(candidate -> unique.putIfAbsent(candidate.type + "::" + candidate.title, candidate));

        return new ArrayList<>(unique.values());
    }

    private TransitCandidate toTransitCandidate(JsonNode path) {
        JsonNode info = path.path("info");
        if (!info.isObject()) {
            return null;
        }

        int totalTime = info.path("totalTime").asInt(0);
        if (totalTime <= 0) {
            return null;
        }

        int pathType = path.path("pathType").asInt(0);
        int payment = info.path("payment").asInt(0);
        int busCount = info.path("busTransitCount").asInt(0);
        int subwayCount = info.path("subwayTransitCount").asInt(0);
        int totalWalk = info.path("totalWalk").asInt(0);
        double distanceKm = info.path("totalDistance").asDouble(0.0) / 1000.0;
        String firstStartStation = info.path("firstStartStation").asText("");
        String lastEndStation = info.path("lastEndStation").asText("");
        List<Map<String, Object>> steps = buildTransitSteps(path);
        String title = resolveTransitTitle(pathType, steps, busCount, subwayCount);
        String type = resolveTransitType(pathType, title, busCount, subwayCount);
        String detail = buildTransitDetail(busCount, subwayCount, totalWalk, firstStartStation, lastEndStation);

        return new TransitCandidate(
                type,
                title,
                totalTime,
                payment,
                busCount,
                subwayCount,
                totalWalk,
                distanceKm,
                pathType,
                detail,
                firstStartStation,
                lastEndStation,
                steps
        );
    }

    private List<Map<String, Object>> buildAlternativeMaps(List<TransitCandidate> candidates) {
        Set<String> seenTypes = new LinkedHashSet<>();
        List<Map<String, Object>> items = new ArrayList<>();

        for (TransitCandidate candidate : candidates) {
            String group = normalizeAlternativeGroup(candidate);
            if (!seenTypes.add(group)) {
                continue;
            }
            items.add(candidate.toMap());
            if (items.size() >= 5) {
                break;
            }
        }

        return items;
    }

    private String normalizeAlternativeGroup(TransitCandidate candidate) {
        if ("train".equals(candidate.type)) {
            String title = candidate.title.toLowerCase();
            if (title.contains("ktx") || title.contains("srt")) return "train-ktx";
            if (title.contains("itx")) return "train-itx";
            if (candidate.title.contains("무궁화")) return "train-mugunghwa";
            return "train";
        }
        return candidate.type;
    }

    private String resolveTransitType(int pathType, String title, int busCount, int subwayCount) {
        if (pathType == 11) return "train";
        if (pathType == 12) return "express";
        if (pathType == 14) return "intercity";

        String lower = title.toLowerCase();
        if (lower.contains("ktx") || lower.contains("srt") || lower.contains("itx") || title.contains("무궁화")) {
            return "train";
        }
        if (subwayCount > 0 && busCount == 0) return "subway";
        if (busCount > 0 && subwayCount == 0) return "bus";
        return subwayCount >= busCount ? "subway" : "bus";
    }

    private String resolveTransitTitle(int pathType, List<Map<String, Object>> steps, int busCount, int subwayCount) {
        if (pathType == 11) {
            for (Map<String, Object> step : steps) {
                if ("train".equals(step.get("type"))) {
                    return String.valueOf(step.getOrDefault("title", "열차"));
                }
            }
            return "열차";
        }
        if (pathType == 12) return "고속버스";
        if (pathType == 14) return "시외버스";
        if (subwayCount > 0 && busCount > 0) return "버스 + 지하철";
        if (subwayCount > 0) return "지하철";
        if (busCount > 0) return "버스";
        return "대중교통";
    }

    private String buildTransitDetail(int busCount, int subwayCount, int totalWalk, String firstStartStation, String lastEndStation) {
        List<String> parts = new ArrayList<>();
        if (busCount > 0) parts.add("버스 " + busCount + "회");
        if (subwayCount > 0) parts.add("지하철 " + subwayCount + "회");
        if (totalWalk > 0) parts.add("도보 " + totalWalk + "m");
        if (!firstStartStation.isBlank() && !lastEndStation.isBlank()) {
            parts.add(firstStartStation + " -> " + lastEndStation);
        }
        return String.join(" / ", parts);
    }

    private List<Map<String, Object>> buildTransitSteps(JsonNode path) {
        JsonNode subPath = path.path("subPath");
        if (!subPath.isArray() || subPath.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        for (JsonNode step : subPath) {
            int trafficType = step.path("trafficType").asInt(0);
            int sectionTime = Math.max(1, step.path("sectionTime").asInt(0));
            int distance = step.path("distance").asInt(0);
            int stationCount = step.path("stationCount").asInt(0);
            String startName = step.path("startName").asText("출발");
            String endName = step.path("endName").asText("도착");
            JsonNode lane = step.path("lane").isArray() && !step.path("lane").isEmpty() ? step.path("lane").get(0) : step.path("lane");

            Map<String, Object> item = new LinkedHashMap<>();
            if (trafficType == 3 && lane.isMissingNode()) {
                item.put("type", "walk");
                item.put("title", "도보 이동");
                item.put("summary", startName + " -> " + endName);
                item.put("minutes", sectionTime);
                item.put("durationText", distance > 0 ? distance + "m / " + formatMinutes(sectionTime) : formatMinutes(sectionTime));
            } else if (trafficType == 2) {
                String title = lane.path("busNo").asText(lane.path("name").asText("버스"));
                item.put("type", "bus");
                item.put("title", "버스 " + title);
                item.put("summary", startName + " -> " + endName);
                item.put("minutes", sectionTime);
                item.put("durationText", stationCount > 0 ? stationCount + "정거장 / " + formatMinutes(sectionTime) : formatMinutes(sectionTime));
            } else if (trafficType == 1) {
                String title = lane.path("name").asText("지하철");
                item.put("type", "subway");
                item.put("title", title);
                item.put("summary", startName + " -> " + endName);
                item.put("minutes", sectionTime);
                item.put("durationText", stationCount > 0 ? stationCount + "개 역 / " + formatMinutes(sectionTime) : formatMinutes(sectionTime));
            } else {
                String rawTitle = lane.path("name").asText(lane.path("trainClass").asText("열차"));
                item.put("type", "train");
                item.put("title", normalizeTrainTitle(rawTitle));
                item.put("summary", startName + " -> " + endName);
                item.put("minutes", sectionTime);
                item.put("durationText", stationCount > 0 ? stationCount + "개 역 / " + formatMinutes(sectionTime) : formatMinutes(sectionTime));
            }
            steps.add(item);
        }

        return steps;
    }

    private String normalizeTrainTitle(String rawTitle) {
        String value = safeText(rawTitle, "열차");
        String lower = value.toLowerCase();
        if (lower.contains("ktx") || lower.contains("srt")) return "KTX/SRT";
        if (lower.contains("itx")) return "ITX";
        if (value.contains("무궁화")) return "무궁화호";
        if (value.contains("새마을")) return "새마을호";
        if (value.contains("누리로")) return "누리로";
        return value;
    }

    private int transportTypePriority(String type) {
        return switch (type) {
            case "train" -> 0;
            case "express" -> 1;
            case "intercity" -> 2;
            case "subway" -> 3;
            case "bus" -> 4;
            default -> 9;
        };
    }

    private boolean hasOdsayError(JsonNode response) {
        JsonNode errorNode = response.path("error");
        return !(errorNode.isMissingNode() || errorNode.isNull());
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String resolvePrimaryImage(Accom accom) {
        if (accom == null || accom.getAccomImgList() == null || accom.getAccomImgList().isEmpty()) {
            return "";
        }

        for (AccomImg accomImg : accom.getAccomImgList()) {
            if (accomImg != null && accomImg.getImgUrl() != null && !accomImg.getImgUrl().isBlank()) {
                return accomImg.getImgUrl();
            }
        }

        return "";
    }

    private String formatMinutes(int minutes) {
        if (minutes <= 0) {
            return "0분";
        }

        int hour = minutes / 60;
        int minute = minutes % 60;
        if (hour > 0) {
            return minute > 0 ? hour + "시간 " + minute + "분" : hour + "시간";
        }
        return minute + "분";
    }

    private JsonNode requestJson(String url) throws IOException, InterruptedException {
        return requestJson(url, "GET", null, null);
    }

    private JsonNode requestJsonWithFallback(List<String> urls, String body, String contentType)
            throws IOException, InterruptedException {
        IOException lastException = null;

        for (String url : urls) {
            try {
                return requestJson(url, "POST", body, contentType);
            } catch (IOException e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new IOException("No available upstream endpoint.");
    }

    private JsonNode requestJson(String url, String method, String body, String contentType)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);

        if ("POST".equalsIgnoreCase(method)) {
            builder.header("Content-Type", contentType != null ? contentType : MediaType.TEXT_PLAIN_VALUE);
            builder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
        } else {
            builder.GET();
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Request failed with status " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private List<Map<String, Object>> mapTransportItems(JsonNode elements, double baseLat, double baseLng) {
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (JsonNode element : elements) {
            JsonNode tags = element.path("tags");
            boolean isBusStop = "bus_stop".equals(tags.path("highway").asText());
            boolean isStation = "station".equals(tags.path("railway").asText());

            if (!isBusStop && !isStation) {
                continue;
            }

            double lat = element.path("lat").asDouble(Double.NaN);
            double lng = element.path("lon").asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                continue;
            }

            String type = isBusStop ? "bus" : "subway";
            String name = tags.path("name").asText("").isBlank() ? "이름 없음" : tags.path("name").asText();
            String dedupeKey = type + ":" + name;
            if (!seen.add(dedupeKey)) {
                continue;
            }

            double distance = getDistance(baseLat, baseLng, lat, lng);
            int walkMinutes = Math.max(1, (int) Math.round(distance / 80.0));

            items.add(Map.of(
                    "name", name,
                    "type", type,
                    "lat", lat,
                    "lng", lng,
                    "distance", Math.round(distance),
                    "walkMinutes", walkMinutes
            ));
        }

        items.sort(Comparator.comparingDouble(item -> ((Number) item.get("distance")).doubleValue()));
        return items;
    }

    private double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371e3;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}
