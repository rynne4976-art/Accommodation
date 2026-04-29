package com.Accommodation.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActivityPeriodUtils {

    private static final DateTimeFormatter PERIOD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Pattern PERIOD_DATE_PATTERN = Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}");

    private ActivityPeriodUtils() {
    }

    public static boolean isExpiredEvent(String category, String period, LocalDate today) {
        if (today == null) {
            return false;
        }

        LocalDate endDate = parseEndDate(period);
        return endDate != null && endDate.isBefore(today);
    }

    private static LocalDate parseEndDate(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }

        Matcher matcher = PERIOD_DATE_PATTERN.matcher(period);
        LocalDate endDate = null;
        while (matcher.find()) {
            try {
                endDate = LocalDate.parse(matcher.group(), PERIOD_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return endDate;
    }
}
