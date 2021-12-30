package UrlShortener.conversion;

import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter; 
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.springframework.stereotype.Service;

@Service
public class ConversionService {

	HashMap<String, String> map = new HashMap<>();
	String longAndShortUrlFileName = "longAndShortURL.txt";
	String numberFileName = "number.txt";
	String domain = "https://www.urlShortener.com/";
	String errorMsg = "Invalid URL has been sent for the conversion";
	Long number = 1000000000L;
	
	// constructor
	public ConversionService(){
		initialise();
	}
	
	public void initialise() {
		// Check if longAndShortUrlFileName exists, if not create an empty file
		boolean isPresent1 = checkIfFileExists(longAndShortUrlFileName);
		
		if(isPresent1 == true){
			// fill hashmap using the LongShortUrl
			fillMap(longAndShortUrlFileName, map);
		}
		
		// Check if numberFileName exists, if not create an empty file
		boolean isPresent2 = checkIfFileExists(numberFileName);
		if(isPresent2 == false){
			// fill the numberFileName with 10^9
			fillnumberFileName(numberFileName, number);
		}
		
		// Get number
		number = getNumber(numberFileName);		
	}
	
	// To fill number in the numberFileName
	public void fillnumberFileName(String fileName, Long number){
		try {
			FileWriter writeNumber = new FileWriter(fileName);
			writeNumber.write("" + number); 
			writeNumber.close();
		}  catch(IOException exception){
			System.out.println("An unexpected error is occurred.");  
			exception.printStackTrace(); 
		}
	}
	
	// To read and get number from numberFileName
	public Long getNumber(String fileName){
		Long num = 0L;
		try {
			InputStream inputStream = new FileInputStream(fileName);
	        InputStreamReader inputStreamReader = new InputStreamReader(inputStream); 
	        BufferedReader br = new BufferedReader(inputStreamReader);	
			String line;
	
			while((line = br.readLine()) != null) {
				num = Long.parseLong(line);
			}
			br.close();
		} catch(IOException exception){
			System.out.println("An unexpected error is occurred.");  
			exception.printStackTrace(); 
		}
		
		return num;
	}

	// To check if fileName exists
	public boolean checkIfFileExists(String fileName){
		boolean isPresent = true;
		try{
			File file = new File(fileName);

			if(file.createNewFile() == true){
				isPresent = false;		
			}

		} catch(IOException exception){
			System.out.println("An unexpected error is occurred.");  
			exception.printStackTrace(); 
		}
		return isPresent;
	}
	
	// To fill map with the long url and short url from longAndShortUrlFileName
	public void fillMap(String longAndShortUrlFileName, HashMap<String, String> map){
		try {
			InputStream inputStream = new FileInputStream(longAndShortUrlFileName);
	        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	        BufferedReader br = new BufferedReader(inputStreamReader);	
			String line;
	
			while((line = br.readLine()) != null) {
				
				String[] arrOfStr = line.split(":", 2);
				map.put(arrOfStr[0], arrOfStr[1]);
			}
			br.close();
		} catch(IOException exception){
			System.out.println("An unexpected error is occurred.");  
			exception.printStackTrace(); 		
		}
	}
	
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
	
		// Store Long and Short URL in file
		storeInFile(longAndShortUrlFileName, shortUrl, longUrl);
	
		// Increment number for next long Url
		number += 1;
		
		// Update the number in the file for next iteration
		fillnumberFileName(numberFileName, number);
		
		return domain + map.get(longUrl);
	}
	
	// To store long and short url in longAndShortUrlFileName
	public void storeInFile(String fileName, String shortUrl, String longUrl){
		
		try {
			FileWriter writeFile = new FileWriter(fileName, true);
			writeFile.write(longUrl + ":" + shortUrl + "\n"); 
			writeFile.close();    		
		} catch(IOException exception){
			System.out.println("An unexpected error is occurred.");  
			exception.printStackTrace(); 
		}
	} 	

	// To validate long url
	public boolean validateURL(String longUrl) {
		boolean isValid = false;
		
		if (longUrl.length() > 11 && longUrl.substring(0, 11).equals("http://www."))
			isValid = true;

		else if (longUrl.length() > 12 && longUrl.substring(0, 12).equals("https://www."))
			isValid = true;
		
		return isValid;
	}

	// To sanitize long url
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
		Long num = number;
		
		while(num > 0) {
			shortUrl += str.charAt((int)(num % 62));
			num /= 62;
		}
		
		return shortUrl;
	}
	
	// To store the pair (long Url : short Url) in the map
	public void storeInMap(String shortUrl, String longUrl) {
		map.put(longUrl, shortUrl);
	}
}