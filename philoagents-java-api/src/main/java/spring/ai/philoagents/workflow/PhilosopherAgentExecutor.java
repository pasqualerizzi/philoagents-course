package spring.ai.philoagents.workflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.spring.ai.serializer.std.SpringAIStateSerializer;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import spring.ai.philoagents.services.PhilosopherService;

/**
 * Service responsible for executing philosopher agent workflows, including conversation handling,
 * context summarization, and tool invocation based on user inputs.
 */
public interface PhilosopherAgentExecutor {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PhilosopherAgentExecutor.class);

    /**
     * Calls the Conversation Agent to retrieve conversation details based on user input.
     *
     * @param state The current state of the workflow.
     * @return A map containing the conversation details as output.
     * @throws Exception 
     */
    static Map<String, Object> get_conversation_node(PhilosopherState state, PhilosopherService philosopherService, ChatClient chatClient) throws Exception {
        log.debug("get_conversation_node state: {}", state);
        log.info("get_conversation_node last_message: {}", state.lastMessage().orElse(null));
        String summary = state.getSummary();
        log.debug("get_conversation_node summary: {}", summary);
        var response = philosopherService.getPhilosopherResponse(state, chatClient);
        log.debug("get_conversation_node Output: {}", response);
        if(response.getResult().getOutput().getText().equals(Boolean.TRUE.toString())) {            
            return Map.of("messages", state.lastMessage(), PhilosopherState.SUMMARY_KEY, summary, PhilosopherState.CONTEXT_KEY, "needed");
        }
        return Map.of("messages", response.getResult().getOutput());
    }

    /**
     * Calls the Conversation Agent to retrieve conversation summary based on user input.
     *
     * @param state The current state of the workflow.
     * @return A map containing conversation summary as output.
     * @throws Exception 
     */
    static Map<String, Object> get_summarize_conversation_node(PhilosopherState state, PhilosopherService philosopherService, ChatClient chatClient) throws Exception {
        log.debug("get_summarize_conversation_node state: {}", state);
        log.info("get_summarize_conversation_node last_message: {}", state.lastMessage().orElse(null));
        String summary = state.getSummary();
        log.debug("get_summarize_conversation_node summary: {}", summary);

        var response = philosopherService.getConversationSummary(state, chatClient);
        log.debug("get_summarize_conversation_node Output: {}", response);

        List<Message> messages = state.messages();
        List<Message> trimmedMessages = messages.size() > 5 ? messages.subList(messages.size() - 5, messages.size()) : List.of();
        return Map.of(PhilosopherState.SUMMARY_KEY, response.getResult().getOutput().getText(), "messages", trimmedMessages);
    }

    /**
     * Calls the Context Agent to provide context summary based on user input.
     *
     * @param state The current state of the workflow.
     * @return A map containing context summary as output.
     * @throws Exception 
     */
    static Map<String, Object> get_summarize_context_node(PhilosopherState state, PhilosopherService philosopherService, ChatClient chatClient) throws Exception {
        log.debug("get_summarize_context_node state: {}", state);
        log.info("get_summarize_context_node lastMessage: {}", state.lastMessage().orElse(null));
        String summary = state.getSummary();
        log.debug("get_summarize_context_node summary: {}", summary);
        
        var response = philosopherService.getContextSummary(state.lastMessage().orElse(null).getText(), chatClient);
        log.debug("get_summarize_context_node Output: {}", response);

        return Map.of("messages", response.getResult().getOutput());
    }

    static Map<String, Object> retrieve_philosopher_context(PhilosopherState state, PhilosopherService philosopherService, ChatClient chatClient) throws Exception {
        log.debug("retrieve_philosopher_context state: {}", state);
        log.info("retrieve_philosopher_context lastMessage: {}", state.lastMessage().orElse(null));
        String summary = state.getSummary();
        log.debug("retrieve_philosopher_context summary: {}", summary);

        var response = philosopherService.getPhilosopherContext(state, chatClient);
        log.debug("retrieve_philosopher_context Output: {}", response);

        return Map.of("messages", response.getResult().getOutput(), PhilosopherState.CONTEXT_KEY, response.getResult().getOutput().getText());
    }

    static String toolsCondition(PhilosopherState state){     
        log.debug("toolsCondition state: {}", state);
        log.info("toolsCondition lastMessage: {}", state.lastMessage().orElse(null));
        if (state.getContext().equals("needed") && state.lastMessage().isPresent()) {
            if(state.lastMessage().get() instanceof UserMessage)
                return "tools";
        }
        return "no-tools";
    };

    static Map<String, Object> get_connector_node(PhilosopherState state) {
        log.debug("get_connector_node state: {}", state);
        log.info("get_connector_node lastMessage: {}", state.lastMessage().orElse(null));
        String summary = state.getSummary();
        log.debug("get_connector_node summary: {}", summary);
        return Map.of("messages", state.lastMessage().orElse(null), PhilosopherState.SUMMARY_KEY, summary, PhilosopherState.CONTEXT_KEY, state.getContext());        
    }    

    /**
     * Provides a builder to construct the workflow graph.
     *
     * @return An instance of GraphBuilder.
     */
    static GraphBuilder graphBuilder() {
        return new GraphBuilder();
    }

    /**
     * Builder class to construct a StateGraph for the agent workflow.
     */
    public class GraphBuilder extends AgentExecutorBuilder<GraphBuilder,PhilosopherState>{

        /**
         * Builds the workflow graph by defining nodes and transitions.
         *
         * @return The constructed StateGraph.
         * @throws GraphStateException If the graph cannot be constructed.
         */
        public StateGraph<PhilosopherState> build(PhilosopherService philosopherService) throws GraphStateException {

            if (stateSerializer == null) {
                stateSerializer =  new SpringAIStateSerializer<>(PhilosopherState::new);
            }
            //build chatClient here instead of having it in PhilosopherService and passit to philospherService methods call by call

            Objects.requireNonNull(this.chatModel, "chatModel cannot be null!");
            var toolOptions = ToolCallingChatOptions.builder().internalToolExecutionEnabled(true).build();
            var chatClientBuilder = ChatClient.builder(this.chatModel).defaultOptions(toolOptions)
                    .defaultSystem(this.systemMessage != null ? this.systemMessage
                            : "You are a helpful AI Assistant answering questions.");
/*             if (!this.tools.isEmpty()) {
                chatClientBuilder.defaultToolCallbacks(this.tools);
            } */
            ChatClient chatClient = chatClientBuilder.build();

            return new StateGraph<>(PhilosopherState.SCHEMA, stateSerializer)
                    //define nodes
                    .addNode("conversationNode", node_async(state ->get_conversation_node(state, philosopherService, chatClient)))
                    .addNode("retrieve_philosopher_context", node_async(state -> retrieve_philosopher_context(state, philosopherService, chatClient)))
                    .addNode("summarizeContext", node_async(state -> get_summarize_context_node(state, philosopherService, chatClient)))
                    .addNode("summarizeConversation", node_async(state -> get_summarize_conversation_node(state, philosopherService, chatClient)))
                    .addNode("connector_node", node_async(state -> get_connector_node(state)))
                    //define flow
                    //.addEdge(START, "retrieve_philosopher_context")
                    .addEdge(START, "conversationNode")
                    .addConditionalEdges("conversationNode", edge_async(state -> toolsCondition(state)),  Map.of(
                            "tools", "retrieve_philosopher_context",
                            "no-tools", "connector_node"
                    ))
                    .addEdge("retrieve_philosopher_context", "summarizeContext")
                    .addEdge("summarizeContext", "conversationNode")
                    //.addEdge("summarizeContext", "connector_node")
                    .addConditionalEdges("connector_node", edge_async((EdgeAction<PhilosopherState>) state -> {
                        return (state.messages().size() > 30) ? "summarize" : "end";
                    }), Map.of(
                            "summarize", "summarizeConversation",
                            "end", END
                    ))
                    .addEdge("summarizeConversation", END);
        }
    }
}
