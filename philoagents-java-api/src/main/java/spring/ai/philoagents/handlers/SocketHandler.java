package spring.ai.philoagents.handlers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import spring.ai.philoagents.entities.Philosopher;
import spring.ai.philoagents.entities.PhilosopherFactory;
import spring.ai.philoagents.services.PhilosopherService;
import spring.ai.philoagents.workflow.PhilosopherAgentExecutor;
import spring.ai.philoagents.workflow.PhilosopherState;
import com.google.gson.Gson;

import org.springframework.web.socket.WebSocketSession;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SocketHandler extends TextWebSocketHandler {

    private final OpenAiChatModel chatModel;
    private MemorySaver memorySaver;
    private PhilosopherService philosopherService;
 	List sessions = new CopyOnWriteArrayList<>();

    public SocketHandler(OpenAiChatModel chatModel, MemorySaver memorySaver, PhilosopherService philosopherService) {
        this.chatModel = chatModel;
        this.memorySaver = memorySaver;
        this.philosopherService = philosopherService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("New WebSocket connection established: {}", session.getId());
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Connection closed by {}:{}", session.getRemoteAddress().getHostString(), session.getRemoteAddress().getPort());
        super.afterConnectionClosed(session, status);
    }

 	@Override
    @SneakyThrows
 	public void handleTextMessage(WebSocketSession session, TextMessage message)
 			throws InterruptedException, IOException, GraphStateException {
        session.sendMessage(new TextMessage(new Gson().toJson(Map.of("streaming", true))));
        Map<String, String> value = new Gson().fromJson(message.getPayload(), Map.class);
        log.info("Starting Multi-Agent AI Application");
        String id = value.get("philosopher_id");
        Philosopher philosopher = PhilosopherFactory.getPhilosopher(id);
        var agent = PhilosopherAgentExecutor.graphBuilder().chatModel(chatModel)
                // .toolsFromObject(new RetrievePhilosopherContext())
                .build(philosopherService)
                .compile(CompileConfig.builder().checkpointSaver(memorySaver).releaseThread(false).build());
        var runnableConfig = RunnableConfig.builder().threadId(id).build();
        var result = agent.invoke(Map.<String, Object>of(PhilosopherState.PN_KEY, philosopher.getName(),
                PhilosopherState.PS_KEY, philosopher.getStyle(), PhilosopherState.PP_KEY, philosopher.getPerspective(),
                "messages", new UserMessage(value.get("message"))), runnableConfig).orElseThrow();
        // Use getContent() if available, otherwise fallback to toString()
        String output = result.lastMessage().map(content -> content.getText()).orElse("UNKNOWN");
        log.info(output);
        //chunking the output
        String[] chunks = output.split("(?<=\\G.{100})");
        for (String chunk : chunks) {
            session.sendMessage(new TextMessage(new Gson().toJson(Map.of("chunk", chunk))));
        }
        session.sendMessage(new TextMessage(new Gson().toJson(Map.of("message", output, "streaming", false))));
    }
}
