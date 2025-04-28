package gr.atc.modapto.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import gr.atc.modapto.security.JwtAuthConverter;
import gr.atc.modapto.security.RateLimitingFilter;
import gr.atc.modapto.security.UnauthorizedEntryPoint;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${spring.security.cors.domain}")
    private String rawCorsDomains;

    /**
     * Initialize and Configure Security Filter Chain of HTTP connection
     *
     * @param http       HttpSecurity
     * @param entryPoint UnauthorizedEntryPoint -> To add proper API Response to the
     *                   authorized request
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, UnauthorizedEntryPoint entryPoint)
            throws Exception {
        // Convert JWT Roles with class to Spring Security Roles
        JwtAuthConverter jwtAuthConverter = new JwtAuthConverter();

        // Set Session to Stateless so not to keep any information about the JWT
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure CORS access
                .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))
                // Configure CSRF Token
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new RateLimitingFilter(), SecurityContextHolderFilter.class)
                .exceptionHandling(exc -> exc.authenticationEntryPoint(entryPoint))
                // HTTP Requests authorization properties on URLs
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/api/notification-center/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                // JWT Authentication Configuration
                .oauth2ResourceServer(oauth2ResourceServerCustomizer -> oauth2ResourceServerCustomizer
                        .jwt(jwtCustomizer -> jwtCustomizer.jwtAuthenticationConverter(jwtAuthConverter)));
        return http.build();
    }

    /**
     * Settings for CORS
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        List<String> corsDomains = List.of(rawCorsDomains.split(","));
        log.info("CORS domains: {}", corsDomains);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsDomains);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(86400L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
