package UrlShortener.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import UrlShortener.model.Url;
import UrlShortener.service.ConversionService;

@RestController
public class ConversionController {

    private static final Logger log = LoggerFactory.getLogger(ConversionController.class);

    @Value("${app.domain}")
    private String domain;

    @Autowired
    private ConversionService conversionService;

    @RequestMapping(method = RequestMethod.POST, value = "/conversion")
    public String convert(@RequestBody String longUrl) {
        return conversionService.convert(longUrl);
    }

    @GetMapping("/api/urls")
    public ResponseEntity<List<Map<String, String>>> getAllUrls() {
        List<Url> urls = conversionService.getAllUrls();
        List<Map<String, String>> response = new ArrayList<>();
        for (Url url : urls) {
            Map<String, String> entry = new HashMap<>();
            entry.put("shortCode", url.getShortCode());
            entry.put("longUrl", url.getLongUrl());
            entry.put("shortUrl", domain + url.getShortCode());
            entry.put("createdAt", url.getCreatedAt() != null ? url.getCreatedAt().toString() : "");
            response.add(entry);
        }
        return ResponseEntity.ok(response);
    }

    // 302 redirect: temporary, so browsers don't cache it and analytics still work
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        log.debug("GET /{shortCode} hit. shortCode='{}'", shortCode);
        String forwarded = request.getHeader("X-Forwarded-For");
        String clientIp = (forwarded != null && !forwarded.trim().isEmpty())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        String longUrl = conversionService.getLongUrl(shortCode, clientIp);
        log.debug("Redirecting to: {}", longUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
