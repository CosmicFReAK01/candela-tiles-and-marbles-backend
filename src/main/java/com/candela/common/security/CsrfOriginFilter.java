package com.candela.common.security;

import com.candela.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CsrfOriginFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CsrfOriginFilter(@Value("${app.security.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();
        boolean isMutation = List.of("POST", "PUT", "PATCH", "DELETE").contains(method);

        if (!isMutation) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bearer token requests are safe against ambient browser CSRF
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean hasAuthCookie = false;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("candela_access_token".equals(c.getName())) {
                    hasAuthCookie = true;
                    break;
                }
            }
        }

        if (hasAuthCookie) {
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            String secFetchSite = request.getHeader("Sec-Fetch-Site");

            if ("cross-site".equalsIgnoreCase(secFetchSite)) {
                rejectCsrf(response, "Cross-site request blocked by Sec-Fetch-Site policy");
                return;
            }

            if (origin != null && !isAllowed(origin)) {
                rejectCsrf(response, "Untrusted Origin: " + origin);
                return;
            } else if (origin == null && referer != null) {
                try {
                    String refOrigin = URI.create(referer).getScheme() + "://" + URI.create(referer).getAuthority();
                    if (!isAllowed(refOrigin)) {
                        rejectCsrf(response, "Untrusted Referer: " + refOrigin);
                        return;
                    }
                } catch (Exception e) {
                    rejectCsrf(response, "Malformed Referer header");
                    return;
                }
            } else if (origin == null && referer == null) {
                rejectCsrf(response, "Missing Origin and Referer on cookie-authenticated mutation");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String origin) {
        return allowedOrigins.contains(origin) || allowedOrigins.contains("*");
    }

    private void rejectCsrf(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error("CSRF validation failed: " + message, "CSRF_FORBIDDEN");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
