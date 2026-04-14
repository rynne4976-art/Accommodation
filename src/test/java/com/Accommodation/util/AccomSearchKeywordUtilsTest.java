package com.Accommodation.util;

import com.Accommodation.constant.AccomType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccomSearchKeywordUtilsTest {

    @Test
    void parse_shouldRecognizeCompactRegionAndTypeKeywords() {
        AccomSearchKeywordUtils.SearchKeywords keywords = AccomSearchKeywordUtils.parse("서울호텔");

        assertThat(keywords.getMatchedRegions()).containsExactly("서울");
        assertThat(keywords.getMatchedAccomTypes()).containsExactly(AccomType.HOTEL);
        assertThat(keywords.getCompactQuery()).isEqualTo("서울호텔");
        assertThat(keywords.getTextTokens()).isEmpty();
    }

    @Test
    void parse_shouldExpandRegionalSubAreasToCanonicalRegion() {
        AccomSearchKeywordUtils.SearchKeywords keywords = AccomSearchKeywordUtils.parse("강남 hotel");

        assertThat(keywords.getMatchedRegions()).containsExactly("서울");
        assertThat(keywords.getMatchedAccomTypes()).containsExactly(AccomType.HOTEL);
    }

    @Test
    void parse_shouldKeepFreeTextAfterRemovingKnownKeywords() {
        AccomSearchKeywordUtils.SearchKeywords keywords = AccomSearchKeywordUtils.parse("강릉리조트오션뷰");

        assertThat(keywords.getMatchedRegions()).containsExactly("강릉");
        assertThat(keywords.getMatchedAccomTypes()).containsExactly(AccomType.RESORT);
        assertThat(keywords.getTextTokens()).containsExactly("오션뷰");
    }

    @Test
    void parse_shouldKeepSingleKoreanCharacterWhenItIsMeaningfulRemainder() {
        AccomSearchKeywordUtils.SearchKeywords keywords = AccomSearchKeywordUtils.parse("역호텔");

        assertThat(keywords.getMatchedAccomTypes()).containsExactly(AccomType.HOTEL);
        assertThat(keywords.getTextTokens()).containsExactly("역");
    }
}
