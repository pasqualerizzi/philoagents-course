package spring.ai.philoagents.config;

import java.util.Map;
import java.util.Objects;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.studio.springboot.LangGraphStudioConfig;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import spring.ai.philoagents.services.PhilosopherService;
import spring.ai.philoagents.tools.DateTimeTools;
import spring.ai.philoagents.workflow.PhilosopherAgentExecutor;
import spring.ai.philoagents.workflow.PhilosopherState;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class LangGraphStudioSampleConfig extends LangGraphStudioConfig  {

    @Autowired
    private PhilosopherService philosopherService;

    @Override
    public Map<String, LangGraphStudioServer.Instance> instanceMap() {
        StateGraph<PhilosopherState> workflow = null;
        try {
            workflow = createWorkflow();
        } catch (GraphStateException e) {
            log.error("Failed to create workflow", e);
            throw new RuntimeException(e);
        }
        var instance = LangGraphStudioServer.Instance.builder()
                .title("LangGraph Studio")
                .graph(workflow)
                .addInputStringArg( "messages", true, v -> new UserMessage( Objects.toString(v) ) )
                .addInputStringArg( "philosopher_name", true, v -> v.toString() )
                .build();

        return Map.of("default", instance);
    }

    private StateGraph<PhilosopherState> createWorkflow() throws GraphStateException{
        return PhilosopherAgentExecutor.graphBuilder()
        .toolsFromObject(new DateTimeTools())
        .build(this.philosopherService);
    }   
}
