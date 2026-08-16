package UrlShortener.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import UrlShortener.model.Url;
import UrlShortener.repository.UrlRepository;

@RestController
@RequestMapping("/admin")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    @Value("${app.domain}")
    private String domain;

    @Autowired
    private UrlRepository urlRepository;

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        long totalUrls = urlRepository.count();
        long totalClicks = urlRepository.sumAllClickCounts();
        log.info("Analytics requested. totalUrls={}, totalClicks={}", totalUrls, totalClicks);
        Map<String, Object> response = new HashMap<>();
        response.put("totalUrls", totalUrls);
        response.put("totalClicks", totalClicks);
        response.put("generatedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/urls")
    public ResponseEntity<List<Map<String, Object>>> getUrlAnalytics() {
        List<Url> urls = urlRepository.findAllByOrderByClickCountDesc();
        log.info("URL analytics requested. urlCount={}", urls.size());
        List<Map<String, Object>> response = new ArrayList<>();
        for (Url url : urls) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("shortCode", url.getShortCode());
            entry.put("longUrl", url.getLongUrl());
            entry.put("shortUrl", domain + url.getShortCode());
            entry.put("clickCount", url.getClickCount());
            entry.put("createdAt", url.getCreatedAt() != null ? url.getCreatedAt().toString() : "");
            response.add(entry);
        }
        return ResponseEntity.ok(response);
    }
}
