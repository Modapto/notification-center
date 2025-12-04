package gr.atc.modapto.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles(profiles = "test")
class JwtUtilsTest {
    private static Jwt jwt;

    @BeforeAll
    @SuppressWarnings("unused")
    static void setup() {
        String tokenValue = "mock.jwt.token";
        Map<String, Object> claims = new HashMap<>();
        claims.put("realm_access", Map.of("roles", List.of("SUPER_ADMIN")));
        claims.put("resource_access", Map.of("modapto", Map.of("roles", List.of("ADMIN", "USER"))));
        claims.put("sub", "user123");
        claims.put("pilot_code", "TEST_PILOT");
        claims.put("user_role", "USER_ROLE_TEST");
        claims.put("pilot_role", "PILOT_ROLE_TEST");

        jwt = Jwt.withTokenValue(tokenValue)
                .headers(header -> header.put("alg", "HS256"))
                .claims(claim -> claim.putAll(claims))
                .build();
    }

    @DisplayName("Extract user roles: Success")
    @Test
    void givenJwt_whenExtractUserRoles_thenReturnListOfRoles() {
        // When
        List<String> roles = JwtUtils.extractUserRoles(jwt);

        // Then
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.contains("SUPER_ADMIN"));
    }

    @DisplayName("Extract user roles: Empty when JWT has no roles")
    @Test
    void givenJwtWithoutRoles_whenExtractUserRoles_thenReturnEmptyList() {
        // Given
        Jwt jwtWithoutRoles = Jwt.withTokenValue("token")
                .headers(header -> header.put("alg", "HS256"))
                .claims(claims -> claims.put("realm_access", Map.of()))
                .build();

        // When
        List<String> roles = JwtUtils.extractUserRoles(jwtWithoutRoles);

        // Then
        assertTrue(roles.isEmpty());
    }

    @DisplayName("Extract user roles: Null JWT")
    @Test
    void givenNullJwt_whenExtractUserRoles_thenReturnEmptyList() {
        // When
        List<String> roles = JwtUtils.extractUserRoles(null);

        // Then
        assertTrue(roles.isEmpty());
    }

    @DisplayName("Extract pilot code: Success")
    @Test
    void givenJwt_whenExtractPilotCode_thenReturnPilotCode() {
        // When
        String pilotCode = JwtUtils.extractPilotCode(jwt);

        // Then
        assertNotNull(pilotCode);
        assertEquals("TEST_PILOT", pilotCode);
    }

    @DisplayName("Extract pilot code: Null when no pilot field")
    @Test
    void givenJwtWithoutPilot_whenExtractPilotCode_thenReturnNull() {
        // Given
        Jwt jwtWithoutPilot = Jwt.withTokenValue("token")
                .headers(header -> header.put("alg", "HS256"))
                .claims(claims -> claims.put("pilot", null))
                .build();

        // When
        String pilotCode = JwtUtils.extractPilotCode(jwtWithoutPilot);

        // Then
        assertNull(pilotCode);
    }

    @DisplayName("Extract user ID: Success")
    @Test
    void givenJwt_whenExtractUserId_thenReturnUserId() {
        // When
        String userId = JwtUtils.extractUserId(jwt);

        // Then
        assertNotNull(userId);
        assertEquals("user123", userId);
    }

    @DisplayName("Extract user ID: Null when no ID field")
    @Test
    void givenJwtWithoutUserId_whenExtractUserId_thenReturnNull() {
        // Given
        Jwt jwtWithoutUserId = Jwt.withTokenValue("token")
                .headers(header -> header.put("alg", "HS256"))
                .claims(claims -> claims.put("sub", null))
                .build();

        // When
        String userId = JwtUtils.extractUserId(jwtWithoutUserId);

        // Then
        assertNull(userId);
    }

    @DisplayName("Extract pilot role: Success")
    @Test
    void givenJwt_whenExtractPilotRole_thenReturnPilotRole() {
        // When
        String pilotRole = JwtUtils.extractPilotRole(jwt);

        // Then
        assertNotNull(pilotRole);
        assertEquals("PILOT_ROLE_TEST", pilotRole);
    }

    @DisplayName("Extract user role: Success")
    @Test
    void givenJwt_whenExtractUserCode_thenReturnUserRole() {
        // When
        String userRole = JwtUtils.extractUserRole(jwt);

        // Then
        assertNotNull(userRole);
        assertEquals("USER_ROLE_TEST", userRole);
    }

    @DisplayName("Extract pilot role: Null when no pilot role field")
    @Test
    void givenJwtWithoutPilotRole_whenExtractPilotRole_thenReturnNull() {
        // Given
        Jwt jwtWithoutPilotRole = Jwt.withTokenValue("token")
                .headers(header -> header.put("alg", "HS256"))
                .claims(claims -> claims.put("pilot_role", null))
                .build();

        // When
        String pilotRole = JwtUtils.extractPilotRole(jwtWithoutPilotRole);

        // Then
        assertNull(pilotRole);
    }
}
