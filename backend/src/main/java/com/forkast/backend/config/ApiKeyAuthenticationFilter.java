package com.forkast.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Grants ROLE_ADMIN to requests on /api/admin/** that carry the correct
 * X-Admin-Key.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Admin-Key";

    private final byte[] expectedKey;

    public ApiKeyAuthenticationFilter(String apiKey) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String provided = request.getHeader(HEADER);

        if (provided != null
                && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    "admin-script", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}