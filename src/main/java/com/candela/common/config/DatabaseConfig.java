package com.candela.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url:${DATABASE_URL:}}")
    private String rawUrl;

    @Value("${spring.datasource.username:${DATABASE_USERNAME:}}")
    private String rawUsername;

    @Value("${spring.datasource.password:${DATABASE_PASSWORD:}}")
    private String rawPassword;

    @Value("${spring.datasource.hikari.maximum-pool-size:${DB_MAX_POOL_SIZE:3}}")
    private int maxPoolSize;

    @Bean
    @Primary
    @ConditionalOnMissingBean(type = "org.h2.jdbcx.JdbcDataSource")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("CandelaHikariPool");
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);

        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("CRITICAL: spring.datasource.url or DATABASE_URL must be specified.");
        }

        if (rawUrl.startsWith("jdbc:h2:")) {
            config.setJdbcUrl(rawUrl);
            config.setUsername(rawUsername != null && !rawUsername.isBlank() ? rawUsername : "sa");
            config.setPassword(rawPassword != null ? rawPassword : "");
            config.setDriverClassName("org.h2.Driver");
            return new HikariDataSource(config);
        }

        if (rawUrl.startsWith("jdbc:postgresql:")) {
            config.setJdbcUrl(rawUrl);
            config.setUsername(rawUsername);
            config.setPassword(rawPassword);
            config.setDriverClassName("org.postgresql.Driver");
            return new HikariDataSource(config);
        }

        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = URI.create(rawUrl);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();

                String userInfo = uri.getUserInfo();
                String username = rawUsername;
                String password = rawPassword;

                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }

                String query = uri.getQuery();
                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path)
                        .append("?sslmode=require&prepareThreshold=0");

                if (query != null && !query.isBlank()) {
                    for (String param : query.split("&")) {
                        if (!param.startsWith("sslmode=") && !param.startsWith("prepareThreshold=")) {
                            jdbcUrl.append("&").append(param);
                        }
                    }
                }

                log.info("Configured PostgreSQL JDBC DataSource connected to {}:{}", host, port);
                config.setJdbcUrl(jdbcUrl.toString());
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");
                return new HikariDataSource(config);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse DATABASE_URL into JDBC format: " + e.getMessage(), e);
            }
        }

        throw new IllegalArgumentException("Unsupported datasource URL format: " + rawUrl);
    }
}
