package fr.siovision.voyages.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProps(
        Rp rp, Jwt jwt, Challenge challenge, Otp otp
){
    public record Rp(String id, String name, String origin){}
    public record Jwt(String issuer, long accessTtlSeconds, long pendingTtlSeconds, String hmacSecret){ }
    public record Challenge(long ttlSeconds, long timeoutMs){}
    public record Otp(int length, long ttlSeconds){}
}