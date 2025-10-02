package spring.ai.philoagents.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import spring.ai.philoagents.repository.PhilosopherSearchRepository;
import spring.ai.philoagents.tools.RetrievePhilosopherContext;
import spring.ai.philoagents.workflow.PhilosopherState;

import org.springframework.ai.document.Document;

@Service
public class PhilosopherService {
    // This service can be used to implement business logic related to philosophers
    // For example, it could interact with a database or perform complex
    // calculations
    // related to philosophical concepts or philosophers' works.

    // Currently, this service is empty, but it can be expanded as needed.

    @Value("classpath:/prompts/philosopher_response.st")
    private Resource philosopher_response_resource;

    @Value("classpath:/prompts/guest_response.st")
    private Resource guest_response_resource;

    @Value("classpath:/prompts/conversation_summary.st")
    private Resource conversation_summary_resource;

    @Value("classpath:/prompts/context_summary.st")
    private Resource context_summary_resource;

    private final VectorStore vectorStore;
    private final RetrievePhilosopherContext retrievePhilosopherContext;
    private final PhilosopherSearchRepository philosopherSearchRepository;

    public PhilosopherService(VectorStore vectorStore, RetrievePhilosopherContext retrievePhilosopherContext,
            PhilosopherSearchRepository philosopherSearchRepository) {
        this.vectorStore = vectorStore;
        this.retrievePhilosopherContext = retrievePhilosopherContext;
        this.philosopherSearchRepository = philosopherSearchRepository;
    }

    public ChatResponse getPhilosopherResponse(PhilosopherState state, ChatClient chatClient) throws Exception {
        List<Message> promptMessages = new java.util.ArrayList<>();
        if (state.getPN().equals("Pasquale Rizzi")) {
            Message message = new SystemPromptTemplate(guest_response_resource).createMessage(
                    Map.of("philosopher_name", state.getPN(), "philosopher_perspective", state.getPP(),
                            "philosopher_style",
                            state.getPS(), "summary", state.getSummary(), "messages", state.messages()));
            RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(this.vectorStore).build())
                    .build();
            promptMessages.add(message);
            promptMessages.addAll(state.messages());
            return chatClient
                    .prompt(new Prompt(promptMessages))
                    .advisors(ragAdvisor)
                    .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                            "philosopher_id == 'posapiano'"))
                    .call()
                    .chatResponse();

        } else {
            Message message = new SystemPromptTemplate(philosopher_response_resource).createMessage(
                    Map.of("philosopher_name", state.getPN(), "philosopher_perspective", state.getPP(),
                            "philosopher_style",
                            state.getPS(), "summary", state.getSummary(), "messages", state.messages()));
            promptMessages.add(message);
            promptMessages.addAll(state.messages());
            return chatClient.prompt(new Prompt(promptMessages)).tools(retrievePhilosopherContext).call()
                    .chatResponse();
        }
    }

    public ChatResponse getConversationSummary(PhilosopherState state, ChatClient chatClient) throws Exception {
        String summary = state.getSummary();
        Message message;
        if (summary == null || summary.isEmpty()) {
            message = new PromptTemplate(
                    "Create a summary of the conversation between {{philosopher_name}} and the user.\r\n" + //
                            "The summary must be a short description of the conversation so far, but that also captures all the\r\n"
                            + //
                            "relevant information shared between {philosopher_name} and the user: ")
                    .createMessage(Map.of("philosopher_name", state.getPN()));
        } else {
            message = new PromptTemplate(conversation_summary_resource)
                    .createMessage(Map.of("philosopher_name", state.getPN(), "summary", summary));
        }
        List<Message> promptMessages = new java.util.ArrayList<>();
        promptMessages.addAll(state.messages());
        promptMessages.add(message);
        return chatClient.prompt(new Prompt(promptMessages)).call().chatResponse();
    }

    public ChatResponse getContextSummary(String context, ChatClient chatClient) throws Exception {
        var options = OpenAiChatOptions.builder().model("llama-3.1-8b-instant")
                .temperature(1.0)
                .maxCompletionTokens(1024)
                .topP(1.0)
                .build();
        Message message = new PromptTemplate(context_summary_resource)
                .createMessage(Map.of(PhilosopherState.CONTEXT_KEY, context));
        return chatClient.prompt(new Prompt(message, options)).call().chatResponse();
    }

    public ChatResponse getPhilosopherContext(PhilosopherState state, ChatClient chatClient) throws Exception {
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder().documentRetriever(
                VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.80)
                        .topK(2)
                        .vectorStore(vectorStore)
                        .filterExpression(new FilterExpressionBuilder()
                                .eq("philosopher_name", state.getPN())
                                .build())
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
        Advisor safeguardAdvisor = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("confidential", "secret", "internal", "proprietary", "classified")).build();
        /*
         * QuestionAnswerAdvisor questionAnswerAdvisor =
         * QuestionAnswerAdvisor.builder(vectorStore)
         * .searchRequest(
         * SearchRequest.builder().similarityThreshold(0.8d).topK(6)
         * .filterExpression(new FilterExpressionBuilder()
         * .eq("philosopher_name", state.getPN()).build())
         * .build())
         * .build();
         * QueryTransformer transformer = CompressionQueryTransformer.builder()
         * .chatClientBuilder(chatClientBuilder)
         * .build();
         * 
         * Query query = Query.builder()
         * .text("What about premium users?")
         * .history(previousMessages) // Previous conversation context
         * .build();
         * 
         * Query transformedQuery = transformer.transform(query);
         */
        Message lastMessage = state.lastMessage()
                .orElseThrow(() -> new IllegalArgumentException("No last message found"));
        return chatClient.prompt(new Prompt(lastMessage)).advisors(
                // questionAnswerAdvisor
                retrievalAugmentationAdvisor,
                safeguardAdvisor)
                .call().chatResponse();
    }

    /**
     * Performs a semantic search on documents based on the given query, with
     * specified top results and similarity threshold.
     *
     * @param query               The search query
     * @param topK                The number of top results to return
     * @param similarityThreshold The minimum similarity score for results to be
     *                            included
     * @return List of search results containing document content and metadata
     */
    public List<Map<String, Object>> searchPhilosopherInfos(String query) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.50)
                .build();

        List<Document> results = philosopherSearchRepository.semanticSearchByQuery(request);

        return results.stream()
                .map(doc -> Map.of("content", doc.getText(), "metadata", doc.getMetadata()))
                .collect(Collectors.toList());
    }

    /**
     * Searches documents using a metadata filter, such as filtering by artist,
     * alongside the given query.
     *
     * @param query               The search query
     * @param topK                The number of top results to return
     * @param similarityThreshold The minimum similarity score for results to be
     *                            included
     * @param artist              The artist to filter results by
     * @return List of filtered search results containing document content and
     *         metadata
     */
    public List<Map<String, Object>> searchPhilosopherInfosWithFilter(String query, String philosopher_name) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .filterExpression("philosopher_name == '" + philosopher_name + "'")
                .topK(5)
                .similarityThreshold(0.50)
                .build();

        List<Document> results = philosopherSearchRepository.semanticSearchByQuery(request);

        return results.stream()
                .map(doc -> Map.of("content", doc.getText(), "metadata", doc.getMetadata()))
                .collect(Collectors.toList());
    }
}
