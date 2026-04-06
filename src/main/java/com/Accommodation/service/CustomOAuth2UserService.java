package com.Accommodation.service;

import com.Accommodation.config.AuthenticatedMember;
import com.Accommodation.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("google".equals(registrationId)) {
            return loadGoogleUser(oauth2User);
        }

        if ("kakao".equals(registrationId)) {
            return loadKakaoUser(oauth2User);
        }

        throw new OAuth2AuthenticationException(
                new OAuth2Error("unsupported_social_provider"),
                "지원하지 않는 소셜 로그인 제공자입니다."
        );
    }

    private OAuth2User loadGoogleUser(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("sub");

        validateEmail(email, "google_email_missing");

        Member member = memberService.upsertSocialMember("google", email, name, providerId);
        return AuthenticatedMember.from(member, oauth2User.getAttributes());
    }

    @SuppressWarnings("unchecked")
    private OAuth2User loadKakaoUser(OAuth2User oauth2User) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttributes().get("kakao_account");
        Map<String, Object> profile = kakaoAccount == null
                ? null
                : (Map<String, Object>) kakaoAccount.get("profile");
        Map<String, Object> properties = (Map<String, Object>) oauth2User.getAttributes().get("properties");

        String name = null;

        if (profile != null) {
            name = (String) profile.get("nickname");
        }

        if (!StringUtils.hasText(name) && properties != null) {
            name = (String) properties.get("nickname");
        }

        Object idValue = oauth2User.getAttributes().get("id");
        String providerId = idValue == null ? null : String.valueOf(idValue);
        String email = kakaoAccount == null ? null : (String) kakaoAccount.get("email");

        if (!StringUtils.hasText(providerId)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("kakao_id_missing"),
                    "카카오 계정 식별 정보를 가져오지 못했습니다."
            );
        }

        String resolvedEmail = StringUtils.hasText(email)
                ? email
                : "kakao_" + providerId + "@kakao.local";

        Member member = memberService.upsertSocialMember("kakao", resolvedEmail, name, providerId);
        return AuthenticatedMember.from(member, oauth2User.getAttributes());
    }

    private void validateEmail(String email, String errorCode) {
        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(errorCode),
                    "소셜 계정에서 이메일 정보를 가져오지 못했습니다."
            );
        }
    }
}
