package com.Accommodation.service;

import com.Accommodation.config.AuthenticatedMember;
import com.Accommodation.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GoogleOidcUserService {

    private final MemberService memberService;

    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = new OidcUserService().loadUser(userRequest);

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String providerId = oidcUser.getSubject();

        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("google_email_missing"),
                    "구글 계정에서 이메일 정보를 가져오지 못했습니다."
            );
        }

        Member member = memberService.upsertSocialMember("google", email, name, providerId);
        return AuthenticatedMember.from(
                member,
                oidcUser.getAttributes(),
                oidcUser.getClaims(),
                oidcUser.getUserInfo(),
                oidcUser.getIdToken()
        );
    }
}
