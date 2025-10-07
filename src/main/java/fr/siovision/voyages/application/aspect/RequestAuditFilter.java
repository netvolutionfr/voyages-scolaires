package fr.siovision.voyages.application.aspect;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RequestAuditFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            long dur = System.currentTimeMillis() - start;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
            String authorities = (auth != null)
                    ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","))
                    : "";

            String authzHeader = Optional.ofNullable(req.getHeader("Authorization"))
                    .map(h -> h.startsWith("Bearer ") ? "Bearer ****" : h)
                    .orElse("<none>");

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                log.info("REQ {} {} -> {} in {}ms | authz={} | user={} sub={} jti={} roles=[{}]",
                        req.getMethod(), req.getRequestURI(), res.getStatus(), dur,
                        authzHeader, user, jwt.getSubject(), jwt.getId(), authorities);
            } else {
                log.info("REQ {} {} -> {} in {}ms | authz={} | user={} roles=[{}]",
                        req.getMethod(), req.getRequestURI(), res.getStatus(), dur,
                        authzHeader, user, authorities);
            }

            // DEBUG: log des headers HTTP
            // TODO : à enlever en production
            log.warn("⚠️ DEBUG: Logging headers for request to {}", req.getRequestURI());

            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = String.join(", ", Collections.list(req.getHeaders(headerName)));

                // ATTENTION : on loggue même Authorization ici
                log.warn("🟡 Header: {} = {}", headerName, headerValue);
            }
        }
    }
}