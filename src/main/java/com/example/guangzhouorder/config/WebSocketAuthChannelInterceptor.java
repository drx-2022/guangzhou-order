package com.example.guangzhouorder.config;

import com.example.guangzhouorder.security.CustomUserDetailsService;
import com.example.guangzhouorder.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


 //Extracts JWT from STOMP native headers and sets it as the Principal.
 //This makes the authentication available to @MessageMapping handlers via the Principal parameter.

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT from STOMP native headers
            // Client sends it as a custom header in the CONNECT frame
            String token = accessor.getFirstNativeHeader("jwt");
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                try {
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    
                    // THIS is what makes Principal available in @MessageMapping handlers
                    accessor.setUser(auth);
                } catch (Exception e) {
                    // Log but don't fail - let the handler deal with null principal
                    System.err.println("Failed to authenticate WebSocket user: " + e.getMessage());
                }
            }
        }

        return message;
    }
}

