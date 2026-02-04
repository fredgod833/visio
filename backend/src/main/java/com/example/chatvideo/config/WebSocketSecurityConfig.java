package com.example.chatvideo.config;

import com.example.chatvideo.security.jwt.JwtService;
import com.example.chatvideo.security.services.UserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration de sécurité WebSocket avec authentification JWT
 * Intercepte les connexions WebSocket STOMP pour valider le token JWT
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public WebSocketSecurityConfig(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Intercepter uniquement les commandes CONNECT
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.debug("WebSocket CONNECT attempt");

                    // Récupérer le header Authorization
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        log.error("WebSocket connection rejected: Missing or invalid Authorization header");
                        throw new IllegalArgumentException("Missing Authorization header");
                    }

                    // Extraire le token (format: "Bearer <token>")
                    String token = authHeader.substring(7).trim();

                    try {
                        // Valider le token JWT
                        if (!jwtService.validateAccessToken(token)) {
                            log.error("WebSocket connection rejected: Invalid JWT token");
                            throw new IllegalArgumentException("Invalid JWT token");
                        }

                        // Lire le JWT et extraire les informations utilisateur
                        Jwt jwt = jwtService.readJwt(token);
                        String email = jwt.getSubject();

                        // Charger les détails de l'utilisateur
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        // Créer l'authentification
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        token,
                                        userDetails.getAuthorities()
                                );

                        // Associer l'utilisateur authentifié à la session WebSocket
                        accessor.setUser(authentication);

                        log.info("WebSocket connection authenticated for user: {}", email);

                    } catch (Exception e) {
                        log.error("WebSocket authentication failed: {}", e.getMessage());
                        throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
                    }
                }

                return message;
            }
        });
    }
}