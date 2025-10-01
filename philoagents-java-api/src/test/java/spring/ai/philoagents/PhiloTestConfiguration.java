package spring.ai.philoagents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.ollama.OllamaContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PhiloTestConfiguration {

    @Bean
    public OllamaContainer ollamaContainer() {
        return new OllamaContainer("ollama/ollama:0.5.7");
    }

    @Bean
    public DynamicPropertyRegistrar dynamicPropertyRegistrar(OllamaContainer ollamaContainer) {
        return registry -> {
            registry.add("spring.ai.ollama.base-url", ollamaContainer::getEndpoint);
        };
    }

    @Bean
    public ChatClient contentEvaluator(
            OllamaApi olamaApi,
            @Value("${philoagents.evaluation.model}") String evaluationModel) {
        ChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(olamaApi)
                .defaultOptions(OllamaOptions.builder()
                        .model(evaluationModel)
                        .build())
                .modelManagementOptions(ModelManagementOptions.builder()
                        .pullModelStrategy(PullModelStrategy.WHEN_MISSING)
                        .build())
                .build();
        return ChatClient.builder(chatModel)
                .build();
    }

}
