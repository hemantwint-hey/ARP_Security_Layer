package org.example.arp_ecurity_layer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TODO: Configure JWT validation, permit /actuator/health and /actuator/prometheus
        // TODO: Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
        // TODO: Validate same JWT secret as banking-app (org.example.enterprisedigitalbankingsystem)
        throw new UnsupportedOperationException("TODO: implement");
    }
}
