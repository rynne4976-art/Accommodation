package com.Accommodation.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddressUtils {

    private static final Pattern STORED_ADDRESS_PATTERN = Pattern.compile("^\\((\\d{5})\\)\\s*(.*)$");

    private AddressUtils() {
    }

    public static ParsedAddress parseStoredAddress(String storedAddress) {
        if (storedAddress == null || storedAddress.isBlank()) {
            return new ParsedAddress("", "", "");
        }

        Matcher matcher = STORED_ADDRESS_PATTERN.matcher(storedAddress.trim());
        if (!matcher.matches()) {
            return new ParsedAddress("", storedAddress.trim(), "");
        }

        return new ParsedAddress(matcher.group(1), matcher.group(2).trim(), "");
    }

    public static final class ParsedAddress {
        private final String postcode;
        private final String address;
        private final String detailAddress;

        public ParsedAddress(String postcode, String address, String detailAddress) {
            this.postcode = postcode;
            this.address = address;
            this.detailAddress = detailAddress;
        }

        public String getPostcode() {
            return postcode;
        }

        public String getAddress() {
            return address;
        }

        public String getDetailAddress() {
            return detailAddress;
        }
    }
}
