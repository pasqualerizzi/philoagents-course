package spring.ai.philoagents.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class RetrievePhilosopherContext {

    @Tool(
        name = "retrieve_philosopher_context",
        description = "Search and return information about a specific philosopher. Always use this tool when the user asks you about a philosopher, their works, ideas or historical context.",
        returnDirect = true
    )
    boolean toolsCondition() {
        return true;
    }
}
