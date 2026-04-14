package com.Accommodation.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddressUtils {

    private static final Pattern STORED_ADDRESS_PATTERN = Pattern.compile("^\\((\\d{5})\\)\\s*(.*)$");
    private static final String DETAIL_DELIMITER = " || ";
    private static final Pattern LEGACY_ROAD_ADDRESS_PATTERN =
            Pattern.compile("^(.*?(?:길|로)\\s*\\d+(?:-\\d+)?(?:\\s*\\([^)]*\\))?)(?:\\s+(.+))$");
    private static final Pattern LEGACY_JIBUN_ADDRESS_PATTERN =
            Pattern.compile("^(.*?(?:동|읍|면|리)\\s*\\d+(?:-\\d+)?(?:\\s*\\([^)]*\\))?)(?:\\s+(.+))$");

    private AddressUtils() {
    }

    public static String formatStoredAddress(String postcode, String address, String detailAddress) {
        StringBuilder fullAddress = new StringBuilder();

        if (postcode != null && !postcode.isBlank()) {
            fullAddress.append('(').append(postcode.trim()).append(") ");
        }

        if (address != null && !address.isBlank()) {
            fullAddress.append(address.trim());
        }

        if (detailAddress != null && !detailAddress.isBlank()) {
            fullAddress.append(DETAIL_DELIMITER).append(detailAddress.trim());
        }

        return fullAddress.toString().trim();
    }

    public static ParsedAddress parseStoredAddress(String storedAddress) {
        if (storedAddress == null || storedAddress.isBlank()) {
            return new ParsedAddress("", "", "");
        }

        Matcher matcher = STORED_ADDRESS_PATTERN.matcher(storedAddress.trim());
        if (!matcher.matches()) {
            return new ParsedAddress("", storedAddress.trim(), "");
        }

        String postcode = matcher.group(1);
        String body = matcher.group(2).trim();

        int delimiterIndex = body.indexOf(DETAIL_DELIMITER);
        if (delimiterIndex >= 0) {
            String address = body.substring(0, delimiterIndex).trim();
            String detailAddress = body.substring(delimiterIndex + DETAIL_DELIMITER.length()).trim();
            return new ParsedAddress(postcode, address, detailAddress);
        }

        ParsedAddress legacyParsed = parseLegacyAddress(postcode, body);
        if (legacyParsed != null) {
            return legacyParsed;
        }

        return new ParsedAddress(postcode, body, "");
    }

    public static String toDisplayAddress(String storedAddress) {
        ParsedAddress parsedAddress = parseStoredAddress(storedAddress);

        if (parsedAddress.getPostcode().isBlank() && parsedAddress.getAddress().isBlank()) {
            return storedAddress == null ? "" : storedAddress.trim();
        }

        StringBuilder displayAddress = new StringBuilder();
        if (!parsedAddress.getPostcode().isBlank()) {
            displayAddress.append('(').append(parsedAddress.getPostcode()).append(") ");
        }
        if (!parsedAddress.getAddress().isBlank()) {
            displayAddress.append(parsedAddress.getAddress());
        }
        if (!parsedAddress.getDetailAddress().isBlank()) {
            if (displayAddress.length() > 0) {
                displayAddress.append(' ');
            }
            displayAddress.append(parsedAddress.getDetailAddress());
        }
        return displayAddress.toString().trim();
    }

    private static ParsedAddress parseLegacyAddress(String postcode, String body) {
        Matcher roadMatcher = LEGACY_ROAD_ADDRESS_PATTERN.matcher(body);
        if (roadMatcher.matches()) {
            return new ParsedAddress(postcode, roadMatcher.group(1).trim(), roadMatcher.group(2).trim());
        }

        Matcher jibunMatcher = LEGACY_JIBUN_ADDRESS_PATTERN.matcher(body);
        if (jibunMatcher.matches()) {
            return new ParsedAddress(postcode, jibunMatcher.group(1).trim(), jibunMatcher.group(2).trim());
        }

        return null;
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
