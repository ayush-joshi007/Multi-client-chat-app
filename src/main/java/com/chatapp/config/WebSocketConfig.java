package com.chatapp.config;

import com.chatapp.Security.JwtChannelInterceptor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@AllArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtChannelInterceptor jwtChannelInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		log.info("[WS_DEBUG] WebSocketConfig.registerStompEndpoints() - Registering /ws endpoint");
		registry.addEndpoint("/ws")
				.setAllowedOrigins("*");
		log.info("[WS_DEBUG] WebSocketConfig.registerStompEndpoints() - /ws endpoint registered with CORS allowed");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		log.info("[WS_DEBUG] WebSocketConfig.configureClientInboundChannel() - Adding JwtChannelInterceptor");
		registration.interceptors(jwtChannelInterceptor);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config){
		log.info("[WS_DEBUG] WebSocketConfig.configureMessageBroker() - Enabling SimpleBroker for /topic");
		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");
		log.info("[WS_DEBUG] WebSocketConfig.configureMessageBroker() - SimpleBroker enabled, app destination prefix set to /app");
	}
}
