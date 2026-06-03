package com.backend.backend_pfe.config;

import com.backend.backend_pfe.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This class is only responsible for wiring the security filter chain
 *   and related beans. No business logic here.
 *
 * SOLID — Open/Closed Principle (OCP):
 *   New security rules (e.g. role-based access) can be added by
 *   extending the filter chain without modifying existing logic.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users").authenticated()
                        .requestMatchers("/api/evaluations/**").authenticated()
                        // Anomalies V2 — accessible to both RM and CHEF_PROJET
                        .requestMatchers("/api/rm/anomalies-v2/**").hasAnyRole("RESOURCE_MANAGER", "CHEF_PROJET")
                        // Resource Manager endpoints
                        .requestMatchers("/api/rm/**").hasRole("RESOURCE_MANAGER")
                        // Project endpoints — accessible to both CHEF_PROJET and RESOURCE_MANAGER
                        .requestMatchers(HttpMethod.POST, "/api/projets").hasRole("CHEF_PROJET")
                        .requestMatchers(HttpMethod.GET, "/api/projets").hasAnyRole("CHEF_PROJET", "RESOURCE_MANAGER")
                        .requestMatchers("/api/projets/*/previsions/**").hasRole("CHEF_PROJET")
                        .requestMatchers("/api/previsions/**").hasRole("CHEF_PROJET")
                        // Anomalies — accessible to both CHEF_PROJET and RESOURCE_MANAGER
                        .requestMatchers("/api/anomalies/**").hasAnyRole("CHEF_PROJET", "RESOURCE_MANAGER")
                        // KPIs — accessible to RESOURCE_MANAGER
                        .requestMatchers("/api/kpis/**").hasAnyRole("CHEF_PROJET", "RESOURCE_MANAGER")
                        .requestMatchers("/api/collaborateur/**").hasRole("COLLABORATEUR")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
