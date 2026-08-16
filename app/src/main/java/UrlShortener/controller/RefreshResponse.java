package UrlShortener.controller;

public class RefreshResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;

    public RefreshResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
}
