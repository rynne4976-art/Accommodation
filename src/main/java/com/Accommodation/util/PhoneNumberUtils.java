package com.Accommodation.util;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        return phoneNumber.replaceAll("\\D", "");
    }

    public static String format(String phoneNumber) {
        String normalized = normalize(phoneNumber);

        if (normalized == null || normalized.isBlank()) {
            return "";
        }

        if (normalized.length() == 11) {
            return normalized.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
        }

        if (normalized.length() == 10) {
            return normalized.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
        }

        return normalized;
    }
}
