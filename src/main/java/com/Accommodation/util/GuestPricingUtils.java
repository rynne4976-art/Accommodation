package com.Accommodation.util;

import com.Accommodation.constant.AccomType;

/**
 * 숙소 유형별 투숙 인원 제한 및 추가 요금 계산 유틸리티
 *
 * <ul>
 *   <li>호텔·리조트·펜션: 2~10인, 5인째부터 성인 +10% / 아동 +5%</li>
 *   <li>모텔·게스트하우스: 1~6인, 3인째부터 성인 +10% / 아동 +5%</li>
 * </ul>
 */
public final class GuestPricingUtils {

    private GuestPricingUtils() {}

    // ── 유형별 최소 인원 ────────────────────────────────────────────────────
    public static int getMinGuests(AccomType type) {
        return switch (type) {
            case MOTEL, GUESTHOUSE -> 1;
            default -> 2; // HOTEL, RESORT, PENSION
        };
    }

    // ── 유형별 최대 인원 ────────────────────────────────────────────────────
    public static int getMaxGuests(AccomType type) {
        return switch (type) {
            case MOTEL, GUESTHOUSE -> 6;
            default -> 10; // HOTEL, RESORT, PENSION
        };
    }

    // ── 추가 요금 기준 인원 (이 수 초과부터 추가요금 부과) ────────────────
    private static int getSurchargeThreshold(AccomType type) {
        return switch (type) {
            case MOTEL, GUESTHOUSE -> 2; // 3인째부터
            default -> 4;               // 5인째부터 (HOTEL, RESORT, PENSION)
        };
    }

    /**
     * 1박 추가 요금 계산
     *
     * <p>threshold 초과 인원 중 성인을 먼저 배정한 뒤 나머지를 아동으로 처리합니다.</p>
     * <p>예) 호텔(threshold=4), 성인3·아동2 → total=5, 초과1명 → 아동1명 → +5%</p>
     * <p>예) 호텔(threshold=4), 성인5·아동0 → total=5, 초과1명 → 성인1명 → +10%</p>
     *
     * @param type          숙소 유형
     * @param adultCount    성인 수
     * @param childCount    아동 수
     * @param pricePerNight 1박 기본 요금
     * @return              1박 추가 요금 (원)
     */
    public static int calculateSurchargePerNight(AccomType type,
                                                  int adultCount,
                                                  int childCount,
                                                  int pricePerNight) {
        int threshold = getSurchargeThreshold(type);
        int total = adultCount + childCount;
        if (total <= threshold) {
            return 0;
        }

        int excessTotal  = total - threshold;
        int excessAdults = Math.max(0, adultCount - threshold);
        int excessChildren = excessTotal - excessAdults;

        return (int) (pricePerNight * 0.10 * excessAdults)
             + (int) (pricePerNight * 0.05 * excessChildren);
    }

    /**
     * 인원 유효성 검증 (유형별 최소·최대 인원 및 성인 최소 1명)
     *
     * @throws IllegalArgumentException 조건 위반 시
     */
    public static void validateGuestCount(AccomType type, int adultCount, int childCount) {
        if (adultCount < 1) {
            throw new IllegalArgumentException("성인은 최소 1명 이상이어야 합니다.");
        }

        int total = adultCount + childCount;
        int min = getMinGuests(type);
        int max = getMaxGuests(type);
        String typeName = getTypeName(type);

        if (total < min) {
            throw new IllegalArgumentException(typeName + "은(는) 최소 " + min + "인 이상이어야 합니다.");
        }
        if (total > max) {
            throw new IllegalArgumentException(typeName + "은(는) 최대 " + max + "인까지 가능합니다.");
        }
    }

    // ── 유형 한글명 ──────────────────────────────────────────────────────────
    public static String getTypeName(AccomType type) {
        return switch (type) {
            case HOTEL      -> "호텔";
            case RESORT     -> "리조트";
            case PENSION    -> "펜션";
            case MOTEL      -> "모텔";
            case GUESTHOUSE -> "게스트하우스";
        };
    }
}
