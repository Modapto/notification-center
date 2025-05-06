package gr.atc.modapto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSocketCorsConfig implements WebMvcConfigurer {

    @Value("${spring.security.cors.domains}")
    private String rawCorsDomains;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = rawCorsDomains.split(",");

        registry.addMapping("/notifications/websocket/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}