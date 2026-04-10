package com.Accommodation.controller;

import com.Accommodation.dto.AccomSearchDto;
import com.Accommodation.dto.MainAccomDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.service.AccomService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
    private final ObjectMapper objectMapper;

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
            @RequestParam(value = "myAddress", required = false) String myAddress,
            HttpSession session,
            Model model
    ) {
        Accom accom = accomId != null ? accomService.getAccomDtl(accomId) : null;
        String resolvedMyPageAddress = resolveMyPageAddress(myAddress, session);

        model.addAttribute("accom", accom);
        model.addAttribute("selectableAccomList", getSelectableAccomList());
        model.addAttribute("odsayWebKey", odsayWebKey);
        model.addAttribute("myPageAddress", resolvedMyPageAddress);

        return "transport/transport";
    }

    private String resolveMyPageAddress(String requestAddress, HttpSession session) {
        if (requestAddress != null && !requestAddress.isBlank()) {
            return requestAddress.trim();
        }

        if (session == null) {
            return "";
        }

        for (String sessionKey : SESSION_USER_KEYS) {
            Object sessionValue = session.getAttribute(sessionKey);
            String extracted = extractAddressDeep(sessionValue, new IdentityHashMap<>());
            if (!extracted.isBlank()) {
                return extracted;
            }
        }

        return "";
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
            return looksLikeAddress(str) ? str.trim() : "";
        }

        String directAddress = extractAddressFromObject(target);
        if (!directAddress.isBlank()) {
            return directAddress;
        }

        for (String methodName : NESTED_METHOD_NAMES) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object nested = method.invoke(target);
                String extracted = extractAddressDeep(nested, visited);
                if (!extracted.isBlank()) {
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
                if (!extracted.isBlank()) {
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
                if (value instanceof String str && !str.isBlank()) {
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
                if (value instanceof String str && !str.isBlank()) {
                    return str.trim();
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private boolean looksLikeAddress(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            return false;
        }

        return text.contains("시")
                || text.contains("도")
                || text.contains("구")
                || text.contains("군")
                || text.contains("로")
                || text.contains("길")
                || text.contains("동")
                || text.matches(".*\\d{5}.*");
    }

    private List<MainAccomDto> getSelectableAccomList() {
        return accomService.getMainAccomPage(new AccomSearchDto(), PageRequest.of(0, 6)).getContent();
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
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "주변 교통 정보를 불러오지 못했습니다."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "주변 교통 정보를 불러오지 못했습니다."));
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
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "대중교통 API 키가 설정되지 않았습니다."));
        }

        try {
            String url = "https://api.odsay.com/v1/api/searchPubTransPathT"
                    + "?apiKey=" + URLEncoder.encode(odsayApiKey, StandardCharsets.UTF_8)
                    + "&SX=" + startLng
                    + "&SY=" + startLat
                    + "&EX=" + endLng
                    + "&EY=" + endLat
                    + "&lang=0";

            JsonNode response = requestJson(url);
            JsonNode errorNode = response.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                String upstreamMessage = extractOdsayErrorMessage(errorNode);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("message", upstreamMessage));
            }

            JsonNode paths = response.path("result").path("path");
            if (!paths.isArray() || paths.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "대중교통 경로를 찾지 못했습니다."));
            }

            JsonNode bestPath = paths.get(0);
            JsonNode info = bestPath.path("info");

            return ResponseEntity.ok(Map.of(
                    "totalTime", info.path("totalTime").asInt(),
                    "payment", info.path("payment").asInt(),
                    "busTransitCount", info.path("busTransitCount").asInt(),
                    "subwayTransitCount", info.path("subwayTransitCount").asInt(),
                    "firstStartStation", info.path("firstStartStation").asText(""),
                    "lastEndStation", info.path("lastEndStation").asText(""),
                    "totalWalk", info.path("totalWalk").asInt(),
                    "pathType", bestPath.path("pathType").asInt()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "대중교통 경로를 불러오지 못했습니다."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "대중교통 경로를 불러오지 못했습니다."));
        }
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

    private String extractOdsayErrorMessage(JsonNode errorNode) {
        if (errorNode.isArray() && !errorNode.isEmpty()) {
            JsonNode firstError = errorNode.get(0);
            String message = firstError.path("message").asText("");
            if (!message.isBlank()) {
                return "ODsay API 오류: " + message;
            }
        }

        String message = errorNode.path("msg").asText("");
        if (!message.isBlank()) {
            return "ODsay API 오류: " + message;
        }

        return "대중교통 경로를 찾지 못했습니다.";
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