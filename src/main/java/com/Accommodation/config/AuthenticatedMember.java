package com.Accommodation.config;

import com.Accommodation.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class AuthenticatedMember implements UserDetails, OAuth2User, OidcUser {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final Map<String, Object> claims;
    private final OidcUserInfo userInfo;
    private final OidcIdToken idToken;

    private AuthenticatedMember(Member member,
                                Map<String, Object> attributes,
                                Map<String, Object> claims,
                                OidcUserInfo userInfo,
                                OidcIdToken idToken) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.password = member.getPassword();
        this.name = member.getName();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + member.getRole().name())
        );
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.claims = claims == null ? Collections.emptyMap() : claims;
        this.userInfo = userInfo;
        this.idToken = idToken;
    }

    public static AuthenticatedMember from(Member member) {
        return new AuthenticatedMember(member, Collections.emptyMap(), Collections.emptyMap(), null, null);
    }

    public static AuthenticatedMember from(Member member, Map<String, Object> attributes) {
        return new AuthenticatedMember(member, attributes, Collections.emptyMap(), null, null);
    }

    public static AuthenticatedMember from(Member member,
                                           Map<String, Object> attributes,
                                           Map<String, Object> claims,
                                           OidcUserInfo userInfo,
                                           OidcIdToken idToken) {
        return new AuthenticatedMember(member, attributes, claims, userInfo, idToken);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
