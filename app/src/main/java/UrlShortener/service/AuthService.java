package UrlShortener.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-days}")
    private long refreshTokenExpiryDays;

    @Transactional
    public void register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password must not be blank");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException(request.getUsername());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_READ");
        userRepository.save(user);
        log.info("User registered: {}", request.getUsername());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateToken(user.getUsername(), user.getRole());
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(rawRefreshToken);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays));
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in: {}", user.getUsername());
        return new AuthResponse(accessToken, rawRefreshToken, accessTokenExpiryMs / 1000);
    }

    @Transactional
    public RefreshResponse refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(InvalidTokenException::new);

        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException();
        }

        String newAccessToken = jwtService.generateToken(
                token.getUser().getUsername(), token.getUser().getRole());
        log.info("Access token refreshed for user: {}", token.getUser().getUsername());
        return new RefreshResponse(newAccessToken, accessTokenExpiryMs / 1000);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("User logged out: {}", token.getUser().getUsername());
        });
    }
}
