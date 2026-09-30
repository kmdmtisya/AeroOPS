package com.aeroops.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RealmRoleConverterTest {

    private final RealmRoleConverter converter = new RealmRoleConverter();

    @Test
    void mapsRealmAccessRoles_toUppercaseHyphenToUnderscoreRolePrefixedAuthorities() {
        Jwt jwt = jwtWithRealmRoles("controller", "tenant-admin");

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CONTROLLER", "ROLE_TENANT_ADMIN");
    }

    @Test
    void missingRealmAccessClaim_yieldsNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "demo.viewer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt jwtWithRealmRoles(String... roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "demo.user")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
