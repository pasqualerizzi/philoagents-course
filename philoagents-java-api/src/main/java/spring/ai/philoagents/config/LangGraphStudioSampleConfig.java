package spring.ai.philoagents.config;

import java.sql.SQLException;
import java.util.Objects;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.checkpoint.PostgresSaver;
import org.bsc.langgraph4j.spring.ai.serializer.std.SpringAIStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.studio.springboot.AbstractLangGraphStudioConfig;
import org.bsc.langgraph4j.studio.springboot.LangGraphFlow;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import spring.ai.philoagents.services.PhilosopherService;
import spring.ai.philoagents.tools.DateTimeTools;
import spring.ai.philoagents.workflow.PhilosopherAgentExecutor;
import spring.ai.philoagents.workflow.PhilosopherState;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class LangGraphStudioSampleConfig extends AbstractLangGraphStudioConfig {

    final LangGraphFlow flow;
    private PhilosopherService philosopherService;

    @Value("${philoagents.langgraph.postgressaver.host}")
    private String host;
    @Value("${philoagents.langgraph.postgressaver.port}")
    private int port;
    @Value("${philoagents.langgraph.postgressaver.database}")
    private String database;
    @Value("${philoagents.langgraph.postgressaver.username}")
    private String username;
    @Value("${philoagents.langgraph.postgressaver.password}")   
    private String password;
    @Value("${philoagents.langgraph.postgressaver.droptablefirst}")
    private boolean dropTablesFirst;
    @Value("${philoagents.langgraph.postgressaver.createtables}")
    private boolean createTables;

    public LangGraphStudioSampleConfig(ChatModel chatModel, PhilosopherService philosopherService) throws GraphStateException {
        this.philosopherService = philosopherService;
        var workflow = PhilosopherAgentExecutor.graphBuilder().chatModel(chatModel)
        .toolsFromObject(new DateTimeTools())
        .build(this.philosopherService);

        var mermaid = workflow.getGraph( GraphRepresentation.Type.MERMAID, "ReAct Agent", false );
        log.debug( mermaid.content() );
        this.flow = agentWorkflow( workflow );
    }

    private LangGraphFlow agentWorkflow( StateGraph<? extends AgentState> workflow ) throws GraphStateException {        
        return  LangGraphFlow.builder()
                .title("LangGraph Studio (Spring AI)")
                .addInputStringArg( "messages", true, v -> new UserMessage( Objects.toString(v) ) )
                .addInputStringArg( "philosopher_name", true, v -> v.toString() )
                .stateGraph( workflow )
                .compileConfig( CompileConfig.builder()
                        .checkpointSaver( new MemorySaver() )
                        .releaseThread(false)
                        .build())
                .build();

    }    

    @Bean
    MemorySaver getMemorySaver() throws SQLException {
        return PostgresSaver.builder()
            .host(host)
            .port(port)
            .user(username)
            .password(password)
            .database(database)
            .stateSerializer( new SpringAIStateSerializer<>(PhilosopherState::new) )
            .dropTablesFirst(dropTablesFirst)
            .createTables(createTables)
            .build();
    }

    @Override
    public LangGraphFlow getFlow() {
        return this.flow;
    }
}
