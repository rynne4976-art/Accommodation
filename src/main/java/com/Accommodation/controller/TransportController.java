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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

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

    private final AccomService accomService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Value("${tmap.api.key:}")
    private String tmapApiKey;

    @Value("${odsay.api.key:}")
    private String odsayApiKey;

    @Value("${odsay.api.web-key:}")
    private String odsayWebKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @GetMapping("/transport")
    public String transportPage(
            @RequestParam(value = "accomId", required = false) Long accomId,
            HttpSession session,
            Model model
    ) {
        Accom accom = null;
        if (accomId != null) {
            try {
                accom = accomService.getAccomDtl(accomId);
            } catch (EntityNotFoundException ignored) {
            }
        }

        model.addAttribute("accom", accom);
        try {
            model.addAttribute("selectableAccomList", accomService.getTransportSelectableAccomList());
        } catch (Exception ignored) {
            model.addAttribute("selectableAccomList", List.of());
        }
        model.addAttribute("odsayWebKey", odsayWebKey);
        return "transport/transport";
    }

    @GetMapping(value = "/api/transport/reserved-accommodation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> reservedAccommodation(java.security.Principal principal) {
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

        try {
            com.Accommodation.dto.AccomSearchDto searchDto = new com.Accommodation.dto.AccomSearchDto();
            searchDto.setSearchQuery(keyword);
            searchDto.setMinPrice(minPrice);
            searchDto.setMaxPrice(maxPrice);
            searchDto.setMinRating(minRating);
            applyGradeReflectively(searchDto, grade);

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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "숙소 검색에 실패했습니다."));
        }
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        if (!"driving".equalsIgnoreCase(profile)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "자동차 경로만 지원합니다."));
        }

        if (tmapApiKey == null || tmapApiKey.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "TMAP API key is not configured."
            ));
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("startX", String.valueOf(startLng));
            payload.put("startY", String.valueOf(startLat));
            payload.put("endX", String.valueOf(endLng));
            payload.put("endY", String.valueOf(endLat));
            payload.put("reqCoordType", "WGS84GEO");
            payload.put("resCoordType", "WGS84GEO");
            payload.put("searchOption", "0");
            payload.put("trafficInfo", "Y");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://apis.openapi.sk.com/tmap/routes?version=1&format=json"))
                    .timeout(Duration.ofSeconds(20))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("appKey", tmapApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseEntity.ok(Map.of(
                        "available", false,
                        "message", "TMAP driving route lookup failed",
                        "status", response.statusCode()
                ));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "경로를 찾지 못했습니다."));
            }

            JsonNode properties = features.get(0).path("properties");
            Map<String, Object> route = new LinkedHashMap<>();
            route.put("available", true);
            route.put("distance", properties.path("totalDistance").asInt(0));
            route.put("duration", properties.path("totalTime").asInt(0));
            route.put("geometry", buildTmapRouteGeometry(features));
            route.put("source", "tmap");
            return ResponseEntity.ok(route);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "경로를 불러오지 못했습니다."
            ));
        }
    }

    @PostMapping(value = "/api/transport/tmap-transit-route", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> tmapTransitRoute(@RequestBody Map<String, Object> request) {
        if (tmapApiKey == null || tmapApiKey.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "TMAP API key is not configured."
            ));
        }

        try {
            String startX = String.valueOf(request.get("startX"));
            String startY = String.valueOf(request.get("startY"));
            String endX = String.valueOf(request.get("endX"));
            String endY = String.valueOf(request.get("endY"));

            RestTemplate restTemplate = new RestTemplate();

            String url = "https://apis.openapi.sk.com/transit/routes";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", tmapApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("startX", startX);
            body.put("startY", startY);
            body.put("endX", endX);
            body.put("endY", endY);
            body.put("count", 5);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> result = response.getBody();

            if (result == null || !result.containsKey("metaData")) {
                return ResponseEntity.ok(Map.of(
                        "available", false,
                        "message", "TMAP 응답 없음"
                ));
            }

            Map<String, Object> metaData = (Map<String, Object>) result.get("metaData");
            Map<String, Object> plan = (Map<String, Object>) metaData.get("plan");
            List<Map<String, Object>> itineraries =
                    plan != null ? (List<Map<String, Object>>) plan.get("itineraries") : null;

            if (itineraries == null || itineraries.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "available", false,
                        "message", "경로 없음"
                ));
            }

            Map<String, Object> best = itineraries.get(0);

            int totalTime = toMinutes(best.get("totalTime"));
            int totalWalk = toMinutes(best.get("totalWalkTime"));
            int transferCount = toInt(best.get("transferCount"));
            int payment = extractFare(best.get("fare"));

            List<Map<String, Object>> legs = (List<Map<String, Object>>) best.get("legs");
            List<Map<String, Object>> steps = new ArrayList<>();

            if (legs != null) {
                for (Map<String, Object> leg : legs) {
                    String mode = String.valueOf(leg.get("mode"));

                    Map<String, Object> step = new LinkedHashMap<>();
                    step.put("type", mode);
                    step.put("title", leg.get("route") != null ? String.valueOf(leg.get("route")) : mode);
                    step.put("summary", buildLegSummary(leg));
                    step.put("minutes", toMinutes(leg.get("sectionTime")));
                    steps.add(step);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "available", true,
                    "totalTime", totalTime,
                    "totalWalk", totalWalk,
                    "transferCount", transferCount,
                    "payment", payment,
                    "steps", steps,
                    "title", "추천 경로"
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", e.getMessage() == null ? "TMAP 대중교통 조회 실패" : e.getMessage()
            ));
        }
    }

    @GetMapping(value = "/api/transport/transit-route", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> odsayTransitRoute(
            @RequestParam("sx") double startLng,
            @RequestParam("sy") double startLat,
            @RequestParam("ex") double endLng,
            @RequestParam("ey") double endLat
    ) {
        if (odsayApiKey == null || odsayApiKey.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", "ODsay API key is not configured."
            ));
        }

        try {
            List<JsonNode> paths = new ArrayList<>();
            String lastOdsayError = "";
            for (String url : buildOdsayTransitUrls(startLng, startLat, endLng, endLat)) {
                try {
                    JsonNode root = requestJson(url);
                    String odsayError = extractOdsayError(root);
                    if (!odsayError.isBlank()) {
                        lastOdsayError = odsayError;
                        continue;
                    }
                    JsonNode pathNode = root.path("result").path("path");
                    if (pathNode.isArray()) {
                        pathNode.forEach(paths::add);
                    }
                } catch (Exception ignored) {
                }
            }

            JsonNode bestPath = paths.stream()
                    .filter(path -> path.path("info").path("totalTime").asInt(0) > 0)
                    .min(Comparator.comparingInt(path -> path.path("info").path("totalTime").asInt(Integer.MAX_VALUE)))
                    .orElse(null);

            if (bestPath == null) {
                return ResponseEntity.ok(buildEstimatedTransitPayload(
                        startLng,
                        startLat,
                        endLng,
                        endLat,
                        lastOdsayError.isBlank() ? "ODsay transit route not found." : lastOdsayError
                ));
            }

            return ResponseEntity.ok(toOdsayTransitPayload(bestPath, paths));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "message", e.getMessage() == null ? "ODsay transit lookup failed." : e.getMessage()
            ));
        }
    }

    private String extractOdsayError(JsonNode root) {
        JsonNode error = root.path("error");
        if (error.isArray() && !error.isEmpty()) {
            JsonNode first = error.get(0);
            String code = first.path("code").asText("");
            String message = first.path("message").asText("");
            return ("ODsay " + code + " " + message).trim();
        }
        if (error.isObject()) {
            String code = error.path("code").asText("");
            String message = error.path("message").asText(error.path("msg").asText(""));
            return ("ODsay " + code + " " + message).trim();
        }
        return "";
    }

    private List<String> buildOdsayTransitUrls(double startLng, double startLat, double endLng, double endLat) {
        String base = "https://api.odsay.com/v1/api/%s?apiKey=%s&SX=%s&SY=%s&EX=%s&EY=%s&OPT=0&lang=0";
        String apiKey = URLEncoder.encode(odsayApiKey, StandardCharsets.UTF_8);
        String common = String.format(
                base,
                "%s",
                apiKey,
                startLng,
                startLat,
                endLng,
                endLat
        );
        return List.of(
                common.formatted("searchPubTransPathT"),
                common.formatted("searchPubTransPathR") + "&SearchType=0",
                common.formatted("searchPubTransPathR") + "&SearchType=1"
        );
    }

    private Map<String, Object> toOdsayTransitPayload(JsonNode path, List<JsonNode> paths) {
        JsonNode info = path.path("info");
        List<Map<String, Object>> steps = buildOdsayTransitSteps(path.path("subPath"), info);
        int pathType = path.path("pathType").asInt(0);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("available", true);
        payload.put("totalTime", info.path("totalTime").asInt(0));
        payload.put("minutes", info.path("totalTime").asInt(0));
        payload.put("payment", info.path("payment").asInt(0));
        payload.put("paymentAmount", info.path("payment").asInt(0));
        payload.put("busTransitCount", info.path("busTransitCount").asInt(0));
        payload.put("subwayTransitCount", info.path("subwayTransitCount").asInt(0));
        payload.put("totalWalk", info.path("totalWalk").asInt(0));
        payload.put("distanceKm", info.path("totalDistance").asDouble(0.0) / 1000.0);
        payload.put("pathType", pathType);
        payload.put("firstStartStation", info.path("firstStartStation").asText(""));
        payload.put("lastEndStation", info.path("lastEndStation").asText(""));
        payload.put("steps", steps);
        payload.put("title", resolveOdsayTransitTitle(pathType, steps));
        payload.put("detail", buildOdsayTransitDetail(info));
        payload.put("alternatives", buildOdsayTransitAlternatives(paths));
        payload.put("geometry", null);
        payload.put("source", "odsay");
        return payload;
    }

    private List<Map<String, Object>> buildOdsayTransitAlternatives(List<JsonNode> paths) {
        List<JsonNode> sortedPaths = paths.stream()
                .filter(path -> path.path("info").path("totalTime").asInt(0) > 0)
                .sorted(Comparator.comparingInt(path -> path.path("info").path("totalTime").asInt(Integer.MAX_VALUE)))
                .limit(5)
                .toList();

        List<Map<String, Object>> alternatives = new ArrayList<>();
        for (int index = 0; index < sortedPaths.size(); index++) {
            JsonNode path = sortedPaths.get(index);
            JsonNode info = path.path("info");
            List<Map<String, Object>> steps = buildOdsayTransitSteps(path.path("subPath"), info);
            int pathType = path.path("pathType").asInt(0);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", resolveOdsayTransitTitle(pathType, steps));
            item.put("totalTime", info.path("totalTime").asInt(0));
            item.put("payment", info.path("payment").asInt(0));
            item.put("pathType", pathType);
            item.put("detail", buildOdsayTransitDetail(info));
            item.put("firstStartStation", info.path("firstStartStation").asText(""));
            item.put("lastEndStation", info.path("lastEndStation").asText(""));
            item.put("steps", steps);
            item.put("fastest", index == 0);
            alternatives.add(item);
        }
        return alternatives;
    }

    private List<Map<String, Object>> buildOdsayTransitSteps(JsonNode subPaths, JsonNode info) {
        if (!subPaths.isArray() || subPaths.isEmpty()) {
            return List.of(Map.of(
                    "type", "transit",
                    "title", "대중교통",
                    "summary", info.path("firstStartStation").asText("출발") + " -> " + info.path("lastEndStation").asText("도착"),
                    "minutes", info.path("totalTime").asInt(0),
                    "durationText", info.path("totalTime").asInt(0) + "분"
            ));
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        for (JsonNode subPath : subPaths) {
            int trafficType = subPath.path("trafficType").asInt(0);
            int minutes = Math.max(1, subPath.path("sectionTime").asInt(0));
            String start = subPath.path("startName").asText("출발");
            String end = subPath.path("endName").asText("도착");
            JsonNode lane = subPath.path("lane").isArray() && !subPath.path("lane").isEmpty()
                    ? subPath.path("lane").get(0)
                    : objectMapper.createObjectNode();

            String type = resolveOdsayStepType(trafficType, lane);
            String title = resolveOdsayStepTitle(type, lane, trafficType);
            String instruction = buildOdsayStepInstruction(type, title, start, end);

            Map<String, Object> step = new LinkedHashMap<>();
            step.put("type", type);
            step.put("title", title);
            step.put("summary", start + " -> " + end);
            step.put("instruction", instruction);
            step.put("startName", start);
            step.put("endName", end);
            step.put("minutes", minutes);
            step.put("durationText", minutes + "분");
            if (subPath.path("stationCount").asInt(0) > 0) {
                step.put("stationCount", subPath.path("stationCount").asInt(0));
            }
            steps.add(step);
        }
        return steps;
    }

    private String buildOdsayStepInstruction(String type, String title, String start, String end) {
        if ("walk".equals(type)) {
            return start + "에서 " + end + "까지 도보 이동";
        }
        String vehicle = title == null || title.isBlank() ? "대중교통" : title;
        return start + "에서 " + vehicle + " 승차 후 " + end + "에서 하차";
    }

    private String resolveOdsayStepType(int trafficType, JsonNode lane) {
        String laneName = lane.path("name").asText("").toLowerCase(Locale.ROOT);
        if (trafficType == 3 || lane.has("trainClass") || laneName.contains("ktx") || laneName.contains("itx") || laneName.contains("srt")) {
            return "train";
        }
        if (trafficType == 1) return "subway";
        if (trafficType == 2 || trafficType == 4 || trafficType == 6) return "bus";
        return "walk";
    }

    private String resolveOdsayStepTitle(String type, JsonNode lane, int trafficType) {
        String busNo = firstText(lane, "busNo", "busNoList", "routeNo", "busNumber");
        String name = firstText(lane, "name", "routeNm", "routeName", "busRouteNm");
        if ("bus".equals(type) && !busNo.isBlank()) {
            return normalizeBusRouteName(busNo, trafficType);
        }
        if (!name.isBlank()) return normalizeTransitVehicleName(name);
        if ("train".equals(type)) return "열차";
        if ("subway".equals(type)) return "지하철";
        if ("bus".equals(type)) return trafficType == 4 || trafficType == 6 ? "고속버스" : "버스";
        return "도보";
    }

    private String normalizeBusRouteName(String value, int trafficType) {
        String text = value == null ? "" : value.trim()
                .replaceAll("\\s+", " ")
                .replaceFirst("^버스\\s*버스\\s*", "버스 ");
        if (text.isBlank()) {
            return trafficType == 4 || trafficType == 6 ? "고속버스" : "버스";
        }
        if (text.startsWith("버스")
                || text.startsWith("간선버스")
                || text.startsWith("지선버스")
                || text.startsWith("광역버스")
                || text.startsWith("마을버스")
                || text.startsWith("공항버스")
                || text.startsWith("고속버스")
                || text.startsWith("시외버스")
                || text.contains("버스")) {
            return text;
        }
        return (trafficType == 4 || trafficType == 6 ? "고속버스 " : "버스 ") + text;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            String text = extractFirstText(value);
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String extractFirstText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = extractFirstText(item);
                if (!text.isBlank()) {
                    return text;
                }
            }
            return "";
        }
        if (node.isObject()) {
            return firstText(node, "busNo", "name", "routeNo", "routeNm", "routeName");
        }
        return node.asText("").trim();
    }

    private String resolveOdsayTransitTitle(int pathType, List<Map<String, Object>> steps) {
        for (Map<String, Object> step : steps) {
            if ("train".equals(step.get("type"))) {
                return String.valueOf(step.getOrDefault("title", "열차"));
            }
        }
        return switch (pathType) {
            case 11 -> "KTX/열차";
            case 12 -> "고속버스";
            case 14 -> "시외버스";
            default -> steps.stream()
                    .filter(step -> !"walk".equals(step.get("type")))
                    .map(step -> String.valueOf(step.get("title")))
                    .findFirst()
                    .orElse("대중교통");
        };
    }

    private String normalizeTransitVehicleName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("ktx")) return "KTX";
        if (lower.contains("srt")) return "SRT";
        if (lower.contains("itx")) return "ITX";
        if (name.contains("무궁화")) return "무궁화호";
        if (name.contains("새마을")) return "새마을호";
        return name;
    }

    private String buildOdsayTransitDetail(JsonNode info) {
        List<String> details = new ArrayList<>();
        int busCount = info.path("busTransitCount").asInt(0);
        int subwayCount = info.path("subwayTransitCount").asInt(0);
        int walk = info.path("totalWalk").asInt(0);
        if (busCount > 0) details.add("버스 " + busCount + "회");
        if (subwayCount > 0) details.add("지하철 " + subwayCount + "회");
        if (walk > 0) details.add("도보 " + walk + "m");
        return String.join(" / ", details);
    }

    private Map<String, Object> buildEstimatedTransitPayload(
            double startLng,
            double startLat,
            double endLng,
            double endLat,
            String reason
    ) {
        double distanceKm = calculateDistanceMeters(startLat, startLng, endLat, endLng) / 1000.0;
        EstimatedTransitMain main = resolveEstimatedTransitMain(distanceKm);
        boolean intercity = main.intercity();
        int accessWalkMinutes = distanceKm >= 10.0 ? 12 : 8;
        int egressWalkMinutes = distanceKm >= 10.0 ? 10 : 6;
        int transferMinutes = main.transferMinutes();
        int mainMinutes = Math.max(main.minMinutes(), (int) Math.round((distanceKm / main.speedKmh()) * 60.0));
        int totalMinutes = accessWalkMinutes + mainMinutes + transferMinutes + egressWalkMinutes;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("available", true);
        payload.put("estimated", true);
        payload.put("source", "estimated");
        payload.put("reason", reason);
        payload.put("totalTime", totalMinutes);
        payload.put("minutes", totalMinutes);
        payload.put("payment", 0);
        payload.put("paymentAmount", 0);
        payload.put("busTransitCount", "bus".equals(main.type()) ? 1 : 0);
        payload.put("subwayTransitCount", "subway".equals(main.type()) ? 1 : 0);
        payload.put("totalWalk", (accessWalkMinutes + egressWalkMinutes) * 70);
        payload.put("distanceKm", distanceKm);
        payload.put("pathType", main.pathType());
        payload.put("firstStartStation", "");
        payload.put("lastEndStation", "");
        payload.put("steps", List.of());
        payload.put("title", main.title());
        payload.put("detail", buildEstimatedTransitDetail(reason));
        payload.put("alternatives", List.of());
        payload.put("geometry", null);
        return payload;
    }

    private String buildEstimatedTransitDetail(String reason) {
        if (reason == null || reason.isBlank()) {
            return "ODsay 조회 실패로 거리 기반 예상값 표시";
        }
        if (reason.contains("ApiKeyAuthFailed")) {
            return "ODsay API 키 인증 실패로 거리 기반 예상값 표시";
        }
        if (reason.toLowerCase(Locale.ROOT).contains("limit") || reason.contains("한도")) {
            return "ODsay API 사용량 제한으로 거리 기반 예상값 표시";
        }
        return "ODsay 조회 실패로 거리 기반 예상값 표시";
    }

    private EstimatedTransitMain resolveEstimatedTransitMain(double distanceKm) {
        if (distanceKm >= 100.0) {
            return new EstimatedTransitMain("train", "장거리 대중교통 예상", 11, true, 190.0, 35, 12, "출발지 주변 주요역/터미널", "도착지 주변 주요역/터미널", "실제 KTX/ITX/고속버스 배차 확인 필요");
        }
        if (distanceKm >= 70.0) {
            return new EstimatedTransitMain("train", "장거리 대중교통 예상", 11, true, 120.0, 30, 14, "출발지 주변 주요역/터미널", "도착지 주변 주요역/터미널", "실제 열차/고속버스 배차 확인 필요");
        }
        if (distanceKm >= 45.0) {
            return new EstimatedTransitMain("bus", "장거리 대중교통 예상", 12, true, 75.0, 35, 16, "출발지 주변 터미널/역", "도착지 주변 터미널/역", "실제 고속버스/철도 배차 확인 필요");
        }
        if (distanceKm >= 12.0) {
            return new EstimatedTransitMain("subway", "지역 대중교통 예상", 3, false, 32.0, 12, 8, "출발지 주변 정류장/역", "도착지 주변 정류장/역", "실제 버스/지하철 노선 확인 필요");
        }
        return new EstimatedTransitMain("bus", "지역 대중교통 예상", 2, false, 24.0, 8, 6, "출발지 주변 정류장", "도착지 주변 정류장", "실제 버스 노선 확인 필요");
    }

    private record EstimatedTransitMain(
            String type,
            String title,
            int pathType,
            boolean intercity,
            double speedKmh,
            int minMinutes,
            int transferMinutes,
            String startHub,
            String endHub,
            String transferSummary
    ) {
    }

    private Map<String, Object> buildEstimatedStep(String type, String title, String summary, int minutes) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("type", type);
        step.put("title", title);
        step.put("summary", summary);
        step.put("minutes", minutes);
        step.put("durationText", minutes + "분");
        return step;
    }

    private String buildLegSummary(Map<String, Object> leg) {
        String start = extractNamedLocation(leg.get("start"), "출발");
        String end = extractNamedLocation(leg.get("end"), "도착");
        return start + " → " + end;
    }

    private String extractNamedLocation(Object raw, String fallback) {
        if (raw instanceof Map<?, ?> map) {
            Object name = map.get("name");
            return name != null ? String.valueOf(name) : fallback;
        }
        return fallback;
    }

    private int extractFare(Object fareObj) {
        if (fareObj instanceof Number n) {
            return n.intValue();
        }
        if (fareObj instanceof Map<?, ?> fareMap) {
            Object regular = fareMap.get("regular");
            if (regular instanceof Map<?, ?> regularMap) {
                Object totalFare = regularMap.get("totalFare");
                return toInt(totalFare);
            }
        }
        return 0;
    }

    private int toMinutes(Object value) {
        int raw = toInt(value);
        if (raw <= 0) return 0;
        return raw > 1000 ? Math.max(1, Math.round(raw / 60f)) : raw;
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value == null) return 0;
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void applyGradeReflectively(Object searchDto, String grade) {
        if (grade == null || grade.isBlank()) return;

        try {
            Class<?> gradeClass = Class.forName("com.Accommodation.constant.Grade");
            @SuppressWarnings("unchecked")
            Object enumValue = Enum.valueOf((Class<? extends Enum>) gradeClass.asSubclass(Enum.class), grade);
            Method method = searchDto.getClass().getMethod("setGrade", gradeClass);
            method.invoke(searchDto, enumValue);
        } catch (Exception ignored) {
        }
    }

    private String resolvePrimaryImage(Accom accom) {
        if (accom == null || accom.getAccomImgList() == null || accom.getAccomImgList().isEmpty()) {
            return "";
        }

        AccomImg first = accom.getAccomImgList().get(0);
        return first != null && first.getImgUrl() != null ? first.getImgUrl() : "";
    }

    private String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private JsonNode requestJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private JsonNode requestJsonWithFallback(List<String> endpoints, String body, String contentType) throws IOException, InterruptedException {
        IOException lastIo = null;
        InterruptedException lastInterrupted = null;

        for (String endpoint : endpoints) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .header("Content-Type", contentType)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return objectMapper.readTree(response.body());
            } catch (IOException e) {
                lastIo = e;
            } catch (InterruptedException e) {
                lastInterrupted = e;
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (lastInterrupted != null) throw lastInterrupted;
        if (lastIo != null) throw lastIo;
        throw new IOException("요청 실패");
    }

    private List<Map<String, Object>> mapTransportItems(JsonNode elements, double baseLat, double baseLng) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (!elements.isArray()) return items;

        for (JsonNode node : elements) {
            double lat = node.path("lat").asDouble();
            double lng = node.path("lon").asDouble();
            String railway = node.path("tags").path("railway").asText("");
            String highway = node.path("tags").path("highway").asText("");
            String name = node.path("tags").path("name").asText("");

            String type = "";
            if ("station".equals(railway)) type = "subway";
            if ("bus_stop".equals(highway)) type = "bus";
            if (type.isBlank()) continue;

            double distance = calculateDistanceMeters(baseLat, baseLng, lat, lng);
            int walkMinutes = Math.max(1, (int) Math.round(distance / 70.0));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", type);
            item.put("name", name == null || name.isBlank() ? ("bus".equals(type) ? "버스 정류장" : "지하철역") : name);
            item.put("lat", lat);
            item.put("lng", lng);
            item.put("distance", (int) Math.round(distance));
            item.put("walkMinutes", walkMinutes);
            items.add(item);
        }

        items.sort(Comparator.comparingInt(item -> ((Number) item.get("distance")).intValue()));
        return items;
    }

    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) *
                                Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLng / 2) *
                                Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c * 1000.0;
    }

    private Map<String, Object> buildTmapRouteGeometry(JsonNode features) {
        List<List<Double>> coordinates = new ArrayList<>();

        for (JsonNode feature : features) {
            JsonNode geometry = feature.path("geometry");
            String type = geometry.path("type").asText("");
            JsonNode geometryCoordinates = geometry.path("coordinates");

            if ("LineString".equals(type) && geometryCoordinates.isArray()) {
                appendTmapLineCoordinates(coordinates, geometryCoordinates);
            } else if ("MultiLineString".equals(type) && geometryCoordinates.isArray()) {
                for (JsonNode line : geometryCoordinates) {
                    appendTmapLineCoordinates(coordinates, line);
                }
            }
        }

        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "LineString");
        geometry.put("coordinates", coordinates);
        return geometry;
    }

    private void appendTmapLineCoordinates(List<List<Double>> coordinates, JsonNode line) {
        if (!line.isArray()) return;

        for (JsonNode coordinate : line) {
            if (!coordinate.isArray() || coordinate.size() < 2) continue;

            List<Double> point = List.of(coordinate.get(0).asDouble(), coordinate.get(1).asDouble());
            if (coordinates.isEmpty() || !coordinates.get(coordinates.size() - 1).equals(point)) {
                coordinates.add(point);
            }
        }
    }
}
