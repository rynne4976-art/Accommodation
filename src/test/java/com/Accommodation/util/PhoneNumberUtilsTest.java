package com.Accommodation.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberUtilsTest {

    @Test
    void normalizeStripsNonDigits() {
        assertThat(PhoneNumberUtils.normalize("010-1234-5678")).isEqualTo("01012345678");
    }

    @Test
    void formatAddsHyphenForElevenDigitNumber() {
        assertThat(PhoneNumberUtils.format("01012345678")).isEqualTo("010-1234-5678");
    }

    @Test
    void formatAddsHyphenForTenDigitNumber() {
        assertThat(PhoneNumberUtils.format("0212345678")).isEqualTo("021-234-5678");
    }
}
