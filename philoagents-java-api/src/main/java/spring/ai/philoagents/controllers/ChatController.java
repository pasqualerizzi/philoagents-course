package spring.ai.philoagents.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import spring.ai.philoagents.entities.Philosopher;
import spring.ai.philoagents.entities.PhilosopherFactory;
import spring.ai.philoagents.services.PhilosopherService;
import spring.ai.philoagents.workflow.PhilosopherAgentExecutor;
import spring.ai.philoagents.workflow.PhilosopherState;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import static org.bsc.langgraph4j.utils.CollectionsUtils.entryOf;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@Slf4j
@CrossOrigin(origins = "*") // Allow all origins for CORS
public class ChatController {

  private final OpenAiChatModel chatModel;
  private PhilosopherService philosopherService;

  public ChatController(OpenAiChatModel chatModel,
      PhilosopherService philosopherService) {
    this.chatModel = chatModel;
    this.philosopherService = philosopherService;
  }

  @Tag(name = "REST chat", description = "Chat with agents")
  @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> chat(@RequestBody ChatBody chatBody) throws Exception {
    log.info("Starting Multi-Agent AI Application");
    Philosopher philosopher = PhilosopherFactory.getPhilosopher(chatBody.philosopher_id());
    var agent = PhilosopherAgentExecutor.graphBuilder().chatModel(chatModel).conversationId(philosopher.getId())
        .build(philosopherService)
        .compile(CompileConfig.builder()
            .build());
    var runnableConfig = RunnableConfig.builder().threadId(philosopher.getId()).build();
    var result = agent.invoke(Map.<String, Object>of(
        PhilosopherState.PN_KEY, philosopher.getId(),
        PhilosopherState.PS_KEY, philosopher.getStyle(),
        PhilosopherState.PP_KEY, philosopher.getPerspective(),
        "messages", new UserMessage(chatBody.message())), runnableConfig)
        .orElseThrow();
    // Use getContent() if available, otherwise fallback to toString()
    String output = result.lastMessage().map(content -> content.getText()).orElse("UNKNOWN");
    return new ResponseEntity<Object>(entryOf("response", output), HttpStatusCode.valueOf(200));
  }

  /**
   * Performs a similarity search for philosopher information based on the
   * provided query.
   * 
   * @param philosopher_name The name of the philosopher to filter results.
   * @param query            The search query string.
   * @return A Flux containing a list of maps with philosopher information.
   */
  @Tag(name = "Similarity Search Query", description = "Search for philosopher information from long term memory using RAG")
  @GetMapping("/query")
  public Flux<List<Map<String, Object>>> similaritySearch(@RequestParam(required = true) String philosopher_id,
      @RequestParam String query) {
    log.info("Performing similarity search with philosopher_id: {}, query: {}", philosopher_id, query);
    Philosopher philosopher = PhilosopherFactory.getPhilosopher(philosopher_id);
    return Flux.just(philosopherService.searchPhilosopherInfosWithFilter(query, philosopher.getName()));
  }

  /**
   * Resets the memory of a philosopher releasing its thread (logical delete)
   * 
   * @param philosopher_name
   * @return A Flux containing a confirmation message.
   * @throws Exception
   */
  @Tag(name = "Reset Memory", description = "Reset the memory of a philosopher")
  @GetMapping("/reset-memory")
  public Flux<String> resetMemory(@RequestParam(required = true) String philosopher_name) throws Exception {
    Philosopher philosopher = PhilosopherFactory.getPhilosopher(philosopher_name);
    philosopherService.resetPhilosopherMemory(philosopher.getId());
    return Flux.just("Memory reset");
  }

  public record ChatBody(String message, String philosopher_id) {
    public ChatBody {
      if (message == null || philosopher_id == null) {
        throw new IllegalArgumentException("Message and philosopher_id must not be null");
      }
    }
  }
}