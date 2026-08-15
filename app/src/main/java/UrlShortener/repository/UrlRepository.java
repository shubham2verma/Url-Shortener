package UrlShortener.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import UrlShortener.model.Url;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByLongUrl(String longUrl);

    Optional<Url> findByShortCode(String shortCode);
}
