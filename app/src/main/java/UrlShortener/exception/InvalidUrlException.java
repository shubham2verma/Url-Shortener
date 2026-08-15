package UrlShortener.exception;

public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String url) {
        super("Invalid URL: '" + url + "'. Must start with http://www. or https://www.");
    }
}
