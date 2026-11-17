package com.candela.common.security;

import com.candela.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CsrfOriginFilter csrfOriginFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpStatus.UNAUTHORIZED.value());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiResponse<Void> body = ApiResponse.error("Unauthorized: Invalid or missing token.", "UNAUTHORIZED");
                            res.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpStatus.FORBIDDEN.value());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiResponse<Void> body = ApiResponse.error("Forbidden: Insufficient privileges.", "FORBIDDEN");
                            res.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Auth Endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/auth/login", "/api/auth/refresh").permitAll()

                        // Public Catalogue Reads
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products", "/api/v1/products/{slug}", "/api/v1/products/featured",
                                "/api/products", "/api/products/{slug}", "/api/products/featured",
                                "/api/v1/categories", "/api/v1/categories/{slug}",
                                "/api/categories", "/api/categories/{slug}",
                                "/api/v1/collections", "/api/v1/collections/{slug}",
                                "/api/collections", "/api/collections/{slug}",
                                "/api/v1/search/**", "/api/search/**"
                        ).permitAll()

                        // Public Content & CMS Reads
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/content/homepage", "/api/content/homepage",
                                "/api/v1/content/banners", "/api/content/banners",
                                "/api/v1/content/settings/public", "/api/content/settings/public"
                        ).permitAll()

                        // Public Enquiry Submission & Minimal Tracking
                        .requestMatchers(HttpMethod.POST, "/api/v1/enquiries", "/api/enquiries", "/api/v1/enquiries/track/**", "/api/enquiries/track/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/enquiries/track/**", "/api/enquiries/track/**").permitAll()

                        // Actuator Health
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/startup").permitAll()

                        // Admin Console UI & Routes
                        .requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Catalogue & Product Mutations (Admin & Super Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/**", "/api/products", "/api/products/**", "/api/v1/categories", "/api/v1/categories/**", "/api/categories", "/api/categories/**", "/api/v1/collections", "/api/v1/collections/**", "/api/collections", "/api/collections/**", "/api/v1/admin/categories/**", "/api/v1/admin/collections/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products", "/api/v1/products/**", "/api/products", "/api/products/**", "/api/v1/categories", "/api/v1/categories/**", "/api/categories", "/api/categories/**", "/api/v1/collections", "/api/v1/collections/**", "/api/collections", "/api/collections/**", "/api/v1/admin/categories/**", "/api/v1/admin/collections/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products", "/api/v1/products/**", "/api/products", "/api/products/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products", "/api/v1/products/**", "/api/products", "/api/products/**", "/api/v1/categories", "/api/v1/categories/**", "/api/categories", "/api/categories/**", "/api/v1/collections", "/api/v1/collections/**", "/api/collections", "/api/collections/**", "/api/v1/admin/categories/**", "/api/v1/admin/collections/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // CMS & Content Mutations (Admin & Super Admin)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/content/**", "/api/content/**", "/api/v1/admin/cms/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/content/**", "/api/content/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/content/**", "/api/content/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Media Uploads (Admin & Super Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/media/upload", "/api/media/upload", "/api/v1/media/ingest-url").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/media/**", "/api/media/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Inventory Operations
                        .requestMatchers("/api/v1/inventory/**", "/api/inventory/**").hasAnyRole("INVENTORY_MANAGER", "ADMIN", "SUPER_ADMIN")

                        // Sales & Enquiry Order Management
                        .requestMatchers("/api/v1/admin/sales/**").hasAnyRole("SHOWROOM_ADVISOR", "INVENTORY_MANAGER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/enquiries", "/api/enquiries").hasAnyRole("SHOWROOM_ADVISOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/enquiries/**", "/api/enquiries/**").hasAnyRole("SHOWROOM_ADVISOR", "ADMIN", "SUPER_ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(csrfOriginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
