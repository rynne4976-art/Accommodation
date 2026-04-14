package com.Accommodation.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressUtilsTest {

    @Test
    void formatStoredAddressKeepsDetailDelimiterForPersistence() {
        String stored = AddressUtils.formatStoredAddress("13543", "경기 성남시 분당구 백현동 472-110", "1122");

        assertThat(stored).isEqualTo("(13543) 경기 성남시 분당구 백현동 472-110 || 1122");
    }

    @Test
    void parseStoredAddressRestoresDelimitedAddressParts() {
        AddressUtils.ParsedAddress parsed =
                AddressUtils.parseStoredAddress("(13543) 경기 성남시 분당구 백현동 472-110 || 1122");

        assertThat(parsed.getPostcode()).isEqualTo("13543");
        assertThat(parsed.getAddress()).isEqualTo("경기 성남시 분당구 백현동 472-110");
        assertThat(parsed.getDetailAddress()).isEqualTo("1122");
    }

    @Test
    void parseStoredAddressSplitsLegacyRoadAddress() {
        AddressUtils.ParsedAddress parsed =
                AddressUtils.parseStoredAddress("(06134) 서울 강남구 테헤란로 123 201호");

        assertThat(parsed.getPostcode()).isEqualTo("06134");
        assertThat(parsed.getAddress()).isEqualTo("서울 강남구 테헤란로 123");
        assertThat(parsed.getDetailAddress()).isEqualTo("201호");
    }

    @Test
    void toDisplayAddressRemovesPersistenceDelimiter() {
        String display = AddressUtils.toDisplayAddress("(13543) 경기 성남시 분당구 백현동 472-110 || 1122");

        assertThat(display).isEqualTo("(13543) 경기 성남시 분당구 백현동 472-110 1122");
    }
}
