package fr.siovision.voyages.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProps(
        Jwt jwt, Challenge challenge, Otp otp, Cookie cookie
){
    public record Jwt(String issuer, long accessTtlSeconds, long pendingTtlSeconds){ }
    public record Challenge(long ttlSeconds, long timeoutMs){}
    public record Otp(int length, long ttlSeconds){}
    public record Cookie(boolean secure, String sameSite, String domain){}
}