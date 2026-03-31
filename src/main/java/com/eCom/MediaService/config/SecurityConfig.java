package com.eCom.MediaService.config;

import com.eCom.Commons.config.GatewayAuthFilter;
import com.eCom.Commons.config.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${gateway.auth.secret}")
    private String gatewaySecret;

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtUtil jwtUtil) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll() // Allow all requests (auth handled by filter)
                )
                .addFilterBefore(new GatewayAuthFilter(gatewaySecret, jwtUtil, appName), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
