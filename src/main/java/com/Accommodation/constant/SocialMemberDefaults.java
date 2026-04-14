package com.Accommodation.constant;

public final class SocialMemberDefaults {

    public static final String DEFAULT_NUMBER = "__SOCIAL_DEFAULT_NUMBER__";
    public static final String LEGACY_DEFAULT_NUMBER = "01000000000";
    public static final String DEFAULT_ADDRESS = "(00000) 소셜 로그인 사용자";

    private SocialMemberDefaults() {
    }

    public static boolean isDefaultNumber(String number) {
        return DEFAULT_NUMBER.equals(number);
    }

    public static boolean isDefaultNumber(String number, boolean socialMember) {
        return DEFAULT_NUMBER.equals(number) || (socialMember && LEGACY_DEFAULT_NUMBER.equals(number));
    }
}
