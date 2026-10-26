package in.code2career.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import in.code2career.backend.security.WebSocketAuthorizationInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthorizationInterceptor authorizationInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
            WebSocketAuthorizationInterceptor authorizationInterceptor,
            @Value("${app.cors.allowed-origin:http://localhost:5173}") String allowedOrigin
    ) {
        this.authorizationInterceptor = authorizationInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigin.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authorizationInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-live-status-sockjs")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));

        registry.addEndpoint("/ws-live-status")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}