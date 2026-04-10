package com.Accommodation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ActivityWishKeyUtils {

    private ActivityWishKeyUtils() {
    }

    public static String generateKey(String regionName,
                                     String title,
                                     String address,
                                     String detailUrl,
                                     String externalUrl,
                                     String category) {
        String raw = String.join("|",
                normalize(regionName),
                normalize(title),
                normalize(address),
                normalize(detailUrl),
                normalize(externalUrl),
                normalize(category)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
