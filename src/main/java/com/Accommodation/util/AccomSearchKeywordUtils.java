package com.Accommodation.util;

import com.Accommodation.constant.AccomType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AccomSearchKeywordUtils {

    private static final Map<String, List<String>> REGION_ALIASES = createRegionAliases();
    private static final Map<String, AccomType> TYPE_KEYWORDS = createTypeKeywords();
    private static final List<String> SORTED_REGION_ALIASES = REGION_ALIASES.values()
            .stream()
            .flatMap(Collection::stream)
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();
    private static final List<String> SORTED_TYPE_KEYWORDS = TYPE_KEYWORDS.keySet()
            .stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    private AccomSearchKeywordUtils() {
    }

    public static SearchKeywords parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return SearchKeywords.empty();
        }

        String normalized = normalize(rawQuery);
        String compact = compact(rawQuery);

        Set<String> regions = extractRegions(compact);
        Set<AccomType> accomTypes = extractTypes(compact);
        List<String> textTokens = extractTextTokens(rawQuery, compact, regions, accomTypes);

        return new SearchKeywords(rawQuery.trim(), normalized, compact, regions, accomTypes, textTokens);
    }

    public static List<String> getRegionTerms(String canonicalRegion) {
        return REGION_ALIASES.getOrDefault(canonicalRegion, List.of(canonicalRegion));
    }

    private static Set<String> extractRegions(String compact) {
        Set<String> regions = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : REGION_ALIASES.entrySet()) {
            boolean matched = entry.getValue().stream()
                    .map(AccomSearchKeywordUtils::compact)
                    .anyMatch(alias -> !alias.isEmpty() && compact.contains(alias));
            if (matched) {
                regions.add(entry.getKey());
            }
        }
        return regions;
    }

    private static Set<AccomType> extractTypes(String compact) {
        Set<AccomType> accomTypes = new LinkedHashSet<>();
        for (String keyword : SORTED_TYPE_KEYWORDS) {
            String normalizedKeyword = compact(keyword);
            if (!normalizedKeyword.isEmpty() && compact.contains(normalizedKeyword)) {
                accomTypes.add(TYPE_KEYWORDS.get(keyword));
            }
        }
        return accomTypes;
    }

    private static List<String> extractTextTokens(String rawQuery,
                                                  String compact,
                                                  Set<String> regions,
                                                  Set<AccomType> accomTypes) {
        Set<String> tokens = new LinkedHashSet<>();

        String remainder = compact;
        for (String region : regions) {
            for (String alias : getRegionTerms(region)) {
                String compactAlias = compact(alias);
                if (!compactAlias.isEmpty()) {
                    remainder = remainder.replace(compactAlias, " ");
                }
            }
        }
        for (AccomType accomType : accomTypes) {
            for (Map.Entry<String, AccomType> entry : TYPE_KEYWORDS.entrySet()) {
                if (entry.getValue() == accomType) {
                    String compactKeyword = compact(entry.getKey());
                    if (!compactKeyword.isEmpty()) {
                        remainder = remainder.replace(compactKeyword, " ");
                    }
                }
            }
        }

        for (String piece : remainder.split("\\s+")) {
            String normalizedPiece = normalize(piece);
            if (isMeaningfulFreeText(normalizedPiece)) {
                tokens.add(normalizedPiece);
            }
        }

        tokens.removeIf(String::isBlank);
        return new ArrayList<>(tokens);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String compact(String value) {
        return normalize(value).replace(" ", "");
    }

    private static boolean isMeaningfulFreeText(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (token.length() >= 2) {
            return true;
        }
        return token.matches("[가-힣]");
    }

    private static Map<String, List<String>> createRegionAliases() {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put("서울", List.of(
                "서울", "서울특별시",
                "강남", "강남구",
                "서초", "서초구",
                "송파", "송파구",
                "잠실",
                "종로", "종로구",
                "명동",
                "홍대",
                "마포", "마포구",
                "여의도", "영등포",
                "용산", "이태원",
                "성수", "건대", "동대문", "구로"
        ));
        aliases.put("부산", List.of(
                "부산", "부산광역시", "해운대", "해운대구", "광안리", "광안", "서면", "남포", "남포동",
                "송정", "기장", "센텀", "센텀시티", "영도"
        ));
        aliases.put("강릉", List.of(
                "강릉", "강릉시", "주문진", "경포", "경포대", "사천", "안목", "정동진", "옥계"
        ));
        aliases.put("제주", List.of(
                "제주", "제주시", "제주도", "서귀포", "서귀포시", "애월", "협재", "한림",
                "성산", "중문", "표선"
        ));
        aliases.put("경주", List.of(
                "경주", "경주시", "보문", "보문단지", "황리단길", "감포", "불국사", "양남"
        ));
        return Collections.unmodifiableMap(aliases);
    }

    private static Map<String, AccomType> createTypeKeywords() {
        Map<String, AccomType> keywords = new LinkedHashMap<>();
        keywords.put("guesthouse", AccomType.GUESTHOUSE);
        keywords.put("guest house", AccomType.GUESTHOUSE);
        keywords.put("게스트하우스", AccomType.GUESTHOUSE);
        keywords.put("게하", AccomType.GUESTHOUSE);
        keywords.put("resort", AccomType.RESORT);
        keywords.put("리조트", AccomType.RESORT);
        keywords.put("pension", AccomType.PENSION);
        keywords.put("펜션", AccomType.PENSION);
        keywords.put("hotel", AccomType.HOTEL);
        keywords.put("호텔", AccomType.HOTEL);
        keywords.put("motel", AccomType.MOTEL);
        keywords.put("모텔", AccomType.MOTEL);
        return Collections.unmodifiableMap(keywords);
    }

    public static final class SearchKeywords {
        private final String originalQuery;
        private final String normalizedQuery;
        private final String compactQuery;
        private final Set<String> matchedRegions;
        private final Set<AccomType> matchedAccomTypes;
        private final List<String> textTokens;

        private SearchKeywords(String originalQuery,
                               String normalizedQuery,
                               String compactQuery,
                               Set<String> matchedRegions,
                               Set<AccomType> matchedAccomTypes,
                               List<String> textTokens) {
            this.originalQuery = originalQuery;
            this.normalizedQuery = normalizedQuery;
            this.compactQuery = compactQuery;
            this.matchedRegions = Collections.unmodifiableSet(new LinkedHashSet<>(matchedRegions));
            this.matchedAccomTypes = Collections.unmodifiableSet(new LinkedHashSet<>(matchedAccomTypes));
            this.textTokens = Collections.unmodifiableList(new ArrayList<>(textTokens));
        }

        public static SearchKeywords empty() {
            return new SearchKeywords("", "", "", Set.of(), Set.of(), List.of());
        }

        public boolean isEmpty() {
            return normalizedQuery.isBlank();
        }

        public String getOriginalQuery() {
            return originalQuery;
        }

        public String getNormalizedQuery() {
            return normalizedQuery;
        }

        public String getCompactQuery() {
            return compactQuery;
        }

        public Set<String> getMatchedRegions() {
            return matchedRegions;
        }

        public Set<AccomType> getMatchedAccomTypes() {
            return matchedAccomTypes;
        }

        public List<String> getTextTokens() {
            return textTokens;
        }
    }
}
