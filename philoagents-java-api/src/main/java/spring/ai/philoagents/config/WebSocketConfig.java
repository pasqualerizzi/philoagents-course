package spring.ai.philoagents.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import spring.ai.philoagents.handlers.BinarySocketHandler;
import spring.ai.philoagents.handlers.SocketHandler;

import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer  {

    @Autowired
    SocketHandler socketHandler;
    @Autowired
    BinarySocketHandler binarySocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, "/ws/chat").setAllowedOrigins("*");
        registry.addHandler(binarySocketHandler, "/ws/chat/binary").setAllowedOrigins("*");
    }


}
