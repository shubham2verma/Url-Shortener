package UrlShortener.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Refresh token is invalid or has expired");
    }
}
