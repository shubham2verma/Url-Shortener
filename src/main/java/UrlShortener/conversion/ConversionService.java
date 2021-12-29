package UrlShortener.conversion;

import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class ConversionService {
	
	HashMap<String, String> map = new HashMap<>();
	Integer number = 1000000000;
	String domain = "https://www.urlShortener.com/";
	String errorMsg = "Invalid URL has been sent for the conversion";
	
	public String convert(String longUrl) {

		// Validate long Url
		boolean isValid = validateURL(longUrl);
		
		if(isValid == false) {
			return errorMsg;
		}
		
		// Sanitize long Url
		longUrl = sanitizeURL(longUrl);
		
		// Check if long Url is already mapped in the map
		if(map.containsKey(longUrl) == true) {
			return domain + map.get(longUrl);
		}
		
		// Generate Short Url for the long Url
		String shortUrl = generate(longUrl);
		
		// Store the pair (long Url : short Url) in the map
		storeInMap(shortUrl, longUrl);
		
		// Increment number for next long Url
		number += 1;
		
		return domain + map.get(longUrl);
	}
	
	public boolean validateURL(String longUrl) {
		boolean isValid = false;
		
		if (longUrl.length() > 11 && longUrl.substring(0, 11).equals("http://www."))
			isValid = true;

		else if (longUrl.length() > 12 && longUrl.substring(0, 12).equals("https://www."))
			isValid = true;
		
		return isValid;
	}
	
	public String sanitizeURL(String url) {
		if (url.substring(0, 7).equals("http://"))
			url = url.substring(7);

		else if (url.substring(0, 8).equals("https://"))
			url = url.substring(8);

		if (url.charAt(url.length() - 1) == '/')
			url = url.substring(0, url.length() - 1);
		
		return url;
	}
	
	// To generate a short url corresponding to the long url 
	public String generate(String longUrl) {
		
		String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String shortUrl = "";
		Integer num = number;
		
		while(num > 0) {
			shortUrl += str.charAt(num % 62);
			num /= 62;
		}
		
		return shortUrl;
	}
	
	// To store the pair (long Url : short Url) in the map
	public void storeInMap(String shortUrl, String longUrl) {
		map.put(longUrl, shortUrl);
	}
	
}
