package UrlShortener.controller;

import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import UrlShortener.service.ConversionService;

@RestController
public class ConversionController {

	@Autowired
	private ConversionService conversionService;

	@RequestMapping(method = RequestMethod.POST, value = "/conversion")
	public String convert(@RequestBody String longUrl) {
		return conversionService.convert(longUrl);
	}

	// 302 redirect: temporary, so browsers don't cache it and analytics still work
	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		System.out.println("[DEBUG] GET /{shortCode} hit. shortCode='" + shortCode + "'");
		String longUrl = conversionService.getLongUrl(shortCode);
		System.out.println("[DEBUG] Redirecting to: " + longUrl);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(longUrl))
				.build();
	}
}
