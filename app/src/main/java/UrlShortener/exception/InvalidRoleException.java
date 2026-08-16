package UrlShortener.exception;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(String role) {
        super("Invalid role: " + role + ". Allowed: ROLE_READ, ROLE_WRITE, ROLE_ADMIN");
    }
}
