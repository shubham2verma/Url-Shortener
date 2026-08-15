package UrlShortener.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import UrlShortener.exception.InvalidUrlException;
import UrlShortener.exception.ShortCodeNotFoundException;
import UrlShortener.model.Url;
import UrlShortener.repository.UrlRepository;

@Service
public class ConversionService {

    private static final String DOMAIN = "https://www.urlShortener.com/";
    private static final long NUMBER_OFFSET = 1_000_000_000L;
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Autowired
    private UrlRepository urlRepository;

    // Returns the original long URL for a given short code, throws 404 if not found
    public String getLongUrl(String shortCode) {
        System.out.println("[DEBUG] getLongUrl called with shortCode: '" + shortCode + "'");
        return urlRepository.findByShortCode(shortCode)
                .map(url -> {
                    String reconstructed = "https://" + url.getLongUrl();
                    System.out.println("[DEBUG] Found in DB. long_url='" + url.getLongUrl() + "' -> redirect to: '" + reconstructed + "'");
                    return reconstructed;
                })
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
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
            return DOMAIN + existing.get().getShortCode();
        }

        // Flush immediately so MySQL assigns the auto-increment ID before we use it
        Url url = urlRepository.saveAndFlush(new Url(longUrl));

        // Derive short code from the DB-generated ID and persist it
        String shortCode = generate(url.getId() + NUMBER_OFFSET);
        System.out.println("[DEBUG] Generated shortCode='" + shortCode + "' for id=" + url.getId());
        url.setShortCode(shortCode);
        urlRepository.saveAndFlush(url);
        System.out.println("[DEBUG] Saved shortCode to DB. Full URL: " + DOMAIN + shortCode);

        return DOMAIN + shortCode;
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
