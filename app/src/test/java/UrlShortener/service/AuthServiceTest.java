package UrlShortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import UrlShortener.controller.AuthResponse;
import UrlShortener.controller.LoginRequest;
import UrlShortener.controller.RefreshResponse;
import UrlShortener.controller.RegisterRequest;
import UrlShortener.exception.InvalidCredentialsException;
import UrlShortener.exception.InvalidTokenException;
import UrlShortener.exception.UserAlreadyExistsException;
import UrlShortener.model.RefreshToken;
import UrlShortener.model.User;
import UrlShortener.repository.RefreshTokenRepository;
import UrlShortener.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // -------------------------------------------------------------------------
    // register() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("register: new username saves user with ROLE_READ")
    void register_withNewUsername_savesUserWithDefaultRole() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiryMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryDays", 7L);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("pass123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("$hashed");

        authService.register(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: duplicate username throws UserAlreadyExistsException")
    void register_withDuplicateUsername_throwsUserAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("pass123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: blank username throws IllegalArgumentException")
    void register_withBlankUsername_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("  ");
        request.setPassword("pass123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // login() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login: valid credentials return AuthResponse with tokens")
    void login_withValidCredentials_returnsAuthResponse() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiryMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryDays", 7L);

        User user = new User();
        user.setUsername("alice");
        user.setPassword("$hashed");
        user.setRole("ROLE_WRITE");

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("pass123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "$hashed")).thenReturn(true);
        when(jwtService.generateToken("alice", "ROLE_WRITE")).thenReturn("jwt-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("login: unknown username throws InvalidCredentialsException")
    void login_withUnknownUsername_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("pass");

        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login: wrong password throws InvalidCredentialsException")
    void login_withWrongPassword_throwsInvalidCredentialsException() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("$hashed");

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrongpass");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // -------------------------------------------------------------------------
    // refresh() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("refresh: valid token returns new access token")
    void refresh_withValidToken_returnsNewAccessToken() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiryMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryDays", 7L);

        User user = new User();
        user.setUsername("alice");
        user.setRole("ROLE_READ");

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRevoked(false);
        token.setExpiresAt(LocalDateTime.now().plusDays(6));

        when(refreshTokenRepository.findByToken("valid-uuid")).thenReturn(Optional.of(token));
        when(jwtService.generateToken("alice", "ROLE_READ")).thenReturn("new-jwt");

        RefreshResponse response = authService.refresh("valid-uuid");

        assertThat(response.getAccessToken()).isEqualTo("new-jwt");
    }

    @Test
    @DisplayName("refresh: revoked token throws InvalidTokenException")
    void refresh_withRevokedToken_throwsInvalidTokenException() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);
        token.setExpiresAt(LocalDateTime.now().plusDays(6));

        when(refreshTokenRepository.findByToken("revoked-uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh("revoked-uuid"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("refresh: expired token throws InvalidTokenException")
    void refresh_withExpiredToken_throwsInvalidTokenException() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("expired-uuid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh("expired-uuid"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("refresh: unknown token throws InvalidTokenException")
    void refresh_withUnknownToken_throwsInvalidTokenException() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // -------------------------------------------------------------------------
    // logout() tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("logout: valid token sets revoked to true")
    void logout_withValidToken_setsRevokedTrue() {
        User user = new User();
        user.setUsername("alice");

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("valid-uuid")).thenReturn(Optional.of(token));

        authService.logout("valid-uuid");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }
}
