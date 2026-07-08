package com.chatapp.Security;

import com.chatapp.service.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private static final String SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    @Nullable
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        // Log every inbound STOMP frame for debugging: command, sessionId, current user, auth header
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (accessor.getCommand() == StompCommand.CONNECT) {
            // Extract the JWT token from the headers and validate it
            if(authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwtToken = authHeader.substring(7);
                try {
                    String extractedUserName = jwtService.extractUserName(jwtToken);
                    if (extractedUserName != null && jwtService.isTokenValid(jwtToken, extractedUserName)) {
                            UserDetails userDetails = customUserDetailsService.loadUserByUsername(extractedUserName);
                            UsernamePasswordAuthenticationToken authentication =
                                UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());

                            //Set authentication in THREE places for proper propagation:
                            // 1. Set user on accessor (for STOMP message header)
                            accessor.setUser(authentication);

                            // 2. Store in WebSocket session attributes (PERSISTENT across all frames in this session)
                            // This is crucial - session attributes survive across CONNECT, SEND, SUBSCRIBE, etc.
                            accessor.getSessionAttributes().put(SECURITY_CONTEXT_KEY, authentication);

                            // 3. Set in SecurityContextHolder (for THIS thread's local context)
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("Authenticated WebSocket session for user '{}'", extractedUserName);
                            return MessageBuilder
                                    .createMessage(message.getPayload(), accessor.getMessageHeaders());
                    }
                    else{
                        log.warn("Rejected WebSocket CONNECT due to invalid JWT");
                        throw new MessagingException("Invalid JWT");
                    }
                } catch (JwtException e) {
                    log.warn("Rejected WebSocket CONNECT due to invalid JWT");
                    throw new MessagingException("Invalid JWT");
                } catch (IllegalArgumentException e) {
                    log.warn("Rejected WebSocket CONNECT due to invalid JWT");
                    throw new MessagingException("Invalid JWT");
                }
            }
            else{
                log.warn("Rejected WebSocket CONNECT due to missing Authorization header");
                throw new MessagingException("Invalid Authorization header");
            }
        }

        // For non-CONNECT frames (SEND, SUBSCRIBE, etc), restore authentication from session attributes
        // Session attributes are persistent across frames in the same WebSocket session
        Authentication storedAuth = (Authentication) accessor.getSessionAttributes().get(SECURITY_CONTEXT_KEY);
        if (storedAuth != null) {
            // Set it both on the accessor and SecurityContextHolder so it's available everywhere
            accessor.setUser(storedAuth);
            SecurityContextHolder.getContext().setAuthentication(storedAuth);
            // Return message with updated accessor headers to persist the user in message headers
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } else if (accessor.getUser() != null) {
            // Fallback: if somehow it's in the accessor, also set in SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication((Authentication) accessor.getUser());
        }

        return message;
    }
}
