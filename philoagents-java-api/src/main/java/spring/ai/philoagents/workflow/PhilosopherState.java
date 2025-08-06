package spring.ai.philoagents.workflow;

import java.util.Map;
import java.util.Optional;

import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.ai.chat.messages.Message;

import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

public class PhilosopherState extends MessagesState<Message>{
    public static final String PN_KEY = "philosopher_name";
    public static final String PP_KEY = "philosopher_perspective";
    public static final String PS_KEY = "philosopher_style";
    public static final String SUMMARY_KEY = "summary";
    public static final String CONTEXT_KEY = "context";

    // Define the schema for the state.
    // MESSAGES_KEY will hold a list of strings, and new messages will be appended.
    public static final Map<String, Channel<?>> SCHEMA = mergeMap(MessagesState.SCHEMA, Map.of(
            PN_KEY, Channels.<String>base(() -> ""),
            PP_KEY, Channels.<String>base(() -> ""),
            PS_KEY, Channels.<String>base(() -> ""),
            SUMMARY_KEY, Channels.<String>base(() -> ""),
            CONTEXT_KEY, Channels.<String>base(() -> ""))
    );

    public PhilosopherState(Map<String, Object> initData) {
        super(initData);
    }

    public String getPN() {
        return (String) value(PN_KEY).orElse("");
    }
    public String getPP() {
        return (String) value(PP_KEY).orElse("");
    }
    public String getPS() {
        return (String) value(PS_KEY).orElse("");
    }
    public String getSummary() {
        return (String) value(SUMMARY_KEY).orElse("");
    }
    public String getContext() {
        return (String) value(CONTEXT_KEY).orElse("");
    }
    public Optional<String> next() {
        return this.value("next");
    }   
}
