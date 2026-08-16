package UrlShortener.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import UrlShortener.model.RefreshToken;
import UrlShortener.model.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}
