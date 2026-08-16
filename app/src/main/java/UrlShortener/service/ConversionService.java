package UrlShortener.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import UrlShortener.exception.InvalidUrlException;
import UrlShortener.exception.ShortCodeNotFoundException;
import UrlShortener.model.Url;
import UrlShortener.repository.UrlRepository;

@Service
public class ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);

    private static final long NUMBER_OFFSET = 1_000_000_000L;
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Value("${app.domain}")
    private String domain;

    @Value("${app.analytics.deduplication.enabled:true}")
    private boolean deduplicationEnabled;

    @Value("${app.analytics.deduplication.strategy:USERNAME}")
    private String deduplicationStrategy;

    @Value("${app.analytics.deduplication.window-minutes:60}")
    private int deduplicationWindowMinutes;

    private final ConcurrentHashMap<String, Long> deduplicationCache = new ConcurrentHashMap<>();

    @Autowired
    private UrlRepository urlRepository;

    // Returns the original long URL for a given short code, throws 404 if not found.
    // Atomically increments the click count as a side effect (unless deduplicated).
    @Transactional
    public String getLongUrl(String shortCode, String clientIp) {
        log.debug("getLongUrl called with shortCode: '{}'", shortCode);
        Optional<Url> optional = urlRepository.findByShortCode(shortCode);
        if (!optional.isPresent()) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        Url url = optional.get();
        if (!isDuplicate(shortCode, clientIp)) {
            urlRepository.incrementClickCount(shortCode);
        }
        String reconstructed = "https://" + url.getLongUrl();
        log.debug("Found in DB. long_url='{}' -> redirect to: '{}'", url.getLongUrl(), reconstructed);
        return reconstructed;
    }

    @Transactional
    public String convert(String longUrl) {

        if (!validateURL(longUrl)) {
            throw new InvalidUrlException(longUrl);
        }

        longUrl = sanitizeURL(longUrl);

        // Return existing short URL if long URL was already shortened
        Optional<Url> existing = urlRepository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            return domain + existing.get().getShortCode();
        }

        // Flush immediately so MySQL assigns the auto-increment ID before we use it
        Url url = urlRepository.saveAndFlush(new Url(longUrl));

        // Derive short code from the DB-generated ID and persist it
        String shortCode = generate(url.getId() + NUMBER_OFFSET);
        log.debug("Generated shortCode='{}' for id={}", shortCode, url.getId());
        url.setShortCode(shortCode);
        urlRepository.saveAndFlush(url);
        log.info("Saved shortCode to DB. Full URL: {}{}", domain, shortCode);

        return domain + shortCode;
    }

    // Returns all shortened URLs stored in the database
    public List<Url> getAllUrls() {
        return urlRepository.findAll();
    }

    // Returns true if this (shortCode, identity) combination was already seen within
    // the deduplication window, false otherwise. Records the visit when returning false.
    private boolean isDuplicate(String shortCode, String clientIp) {
        if (!deduplicationEnabled) return false;

        String key;
        if ("USERNAME".equalsIgnoreCase(deduplicationStrategy)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return false;  // anonymous visitor → always count
            }
            key = auth.getName() + ":" + shortCode;
        } else {
            key = clientIp + ":" + shortCode;
        }

        long now = System.currentTimeMillis();
        Long expiry = deduplicationCache.get(key);
        if (expiry != null && now < expiry) {
            log.debug("Duplicate click suppressed. key='{}'", key);
            return true;
        }
        deduplicationCache.put(key, now + (long) deduplicationWindowMinutes * 60_000L);
        return false;
    }

    // Base-62 encode the given number into a short code
    private String generate(Long num) {
        StringBuilder shortCode = new StringBuilder();
        while (num > 0) {
            shortCode.append(BASE62_CHARS.charAt((int)(num % 62)));
            num /= 62;
        }
        return shortCode.toString();
    }

    // Validate that the URL starts with http://www. or https://www.
    private boolean validateURL(String longUrl) {
        if (longUrl.length() > 11 && longUrl.substring(0, 11).equals("http://www."))
            return true;
        if (longUrl.length() > 12 && longUrl.substring(0, 12).equals("https://www."))
            return true;
        return false;
    }

    // Strip protocol prefix and trailing slash
    private String sanitizeURL(String url) {
        if (url.startsWith("http://"))
            url = url.substring(7);
        else if (url.startsWith("https://"))
            url = url.substring(8);

        if (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        return url;
    }
}
