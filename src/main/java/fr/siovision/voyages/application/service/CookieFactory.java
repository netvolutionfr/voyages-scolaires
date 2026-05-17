package fr.siovision.voyages.application.service;

import fr.siovision.voyages.config.AppProps;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieFactory {

    private final AppProps appProps;

    public ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return build("refresh_token", value, "/api/auth", maxAgeSeconds);
    }

    public ResponseCookie clearRefreshCookie() {
        return build("refresh_token", "", "/api/auth", 0);
    }

    private ResponseCookie build(String name, String value, String path, long maxAge) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(appProps.cookie().secure())
                .sameSite(appProps.cookie().sameSite())
                .path(path)
                .maxAge(maxAge);
        String domain = appProps.cookie().domain();
        if (domain != null && !domain.isBlank()) b.domain(domain);
        return b.build();
    }
}
