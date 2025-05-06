package gr.atc.modapto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.security.cors.domains}")
    private String rawCorsDomains;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/user" , "/queue", "/events");
        config.setApplicationDestinationPrefixes("*");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        String[] corsDomains = rawCorsDomains.split(",");
        log.info("CORS domains for WebSocket: {}", (Object) corsDomains);

        registry.addEndpoint("/notifications/websocket").setAllowedOrigins(corsDomains);
        registry.addEndpoint("/notifications/websocket").setAllowedOrigins(corsDomains).withSockJS();
    }
}