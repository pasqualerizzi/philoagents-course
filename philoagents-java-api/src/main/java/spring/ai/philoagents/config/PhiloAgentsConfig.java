package spring.ai.philoagents.config;

import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class PhiloAgentsConfig {

    @Autowired
    OpenAiChatModel chatModel;

    @Bean
    public ChatClient chatClient() {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        Objects.requireNonNull(this.chatModel, "chatModel cannot be null!");
        var chatClientBuilder = ChatClient.builder(this.chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId("default").build())
                .defaultSystem("You are a helpful AI Assistant answering questions.");
        return chatClientBuilder.build();
    }

}
