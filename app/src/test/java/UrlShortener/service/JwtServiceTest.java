package UrlShortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    // 32+ character secret required for HS256 (256-bit minimum)
    private static final String TEST_SECRET = "test-secret-key-at-least-32-chars-long!";
    private static final long EXPIRY_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", EXPIRY_MS);
    }

    @Test
    @DisplayName("generateToken: returns non-null JWT string")
    void generateToken_returnsNonNullJwtString() {
        String token = jwtService.generateToken("alice", "ROLE_WRITE");

        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("validateToken: valid token returns true")
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtService.generateToken("alice", "ROLE_WRITE");

        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: expired token returns false")
    void validateToken_withExpiredToken_returnsFalse() {
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortLivedService, "accessTokenExpiryMs", 0L);

        String expiredToken = shortLivedService.generateToken("alice", "ROLE_WRITE");

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken: tampered token returns false")
    void validateToken_withTamperedToken_returnsFalse() {
        String token = jwtService.generateToken("alice", "ROLE_WRITE");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("extractUsername: returns correct subject from token")
    void extractUsername_returnsCorrectSubject() {
        String token = jwtService.generateToken("alice", "ROLE_WRITE");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("extractRole: returns correct role claim from token")
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateToken("alice", "ROLE_WRITE");

        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_WRITE");
    }
}
