package com.aeroops.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Keycloak puts realm roles under the nested {@code realm_access.roles} claim, not the flat
 * {@code scope}/{@code scp} claim Spring Security's default JwtGrantedAuthoritiesConverter
 * reads — without this, every JWT would authenticate with zero authorities and every
 * {@code @PreAuthorize("hasRole(...)")} check would fail closed regardless of the user's role.
 */
public class RealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(Object::toString)
                .map(role -> new SimpleGrantedAuthority(
                        "ROLE_" + role.toUpperCase(Locale.ROOT).replace('-', '_')))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
