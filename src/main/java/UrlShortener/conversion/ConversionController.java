package UrlShortener.conversion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ConversionController {
	
	@Autowired
	private ConversionService conversionService;

	@RequestMapping(method=RequestMethod.POST, value="/conversion")
	public String convert(@RequestBody String longUrl) {
		return conversionService.convert(longUrl);
	}
}
