package spring.ai.philoagents;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import lombok.extern.slf4j.Slf4j;
import spring.ai.philoagents.entities.Philosopher;
import spring.ai.philoagents.entities.PhilosopherFactory;
import spring.ai.philoagents.tools.RetrievePhilosopherContext;
import spring.ai.philoagents.workflow.PhilosopherState;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes=TestApplication.class)
public class PhiloAgentsLongTermMemoryTest {

    @Autowired
    private VectorStore vectorStore;
    @Value("classpath:/prompts/philosopher_response.st")
    private Resource philosopher_response_resource;    
	@Autowired
	private ChatModel openAiChatModel;    
    @Autowired
    private RetrievePhilosopherContext retrievePhilosopherContext;

    @Test
    //@Ignore// Ignored because it requires external PDF files to be present in the classpath. Execute it only if GuestPhilosopher data are not part of the collection.
    public void testAddSpecialGuestPhilosopherToVectorStore() throws Exception {
        // Iterate over PDF files in the classpath and pass them to addToVectoreStore
        org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver =
            new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        Resource[] pdfResources = resolver.getResources("classpath*:**/*.pdf");
        for (Resource pdfResource : pdfResources) {
            addToVectoreStore(pdfResource);
        }
    }

    @Test
    public void testSpecialGuestPhilosopherResponse() {
        Philosopher philosopher = PhilosopherFactory.getPhilosopher("posapiano");
        PhilosopherState state = new PhilosopherState(
            Map.of("philosopher_name", philosopher.getName(), "philosopher_perspective", philosopher.getPerspective(), "philosopher_style",  philosopher.getStyle(), "messages", List.of(
            new Message[] {
                new UserMessage("Hello, who are you? What's your goal here? Can you describe me what are your best IT skills and your last IT project?")})));
        Message message = new SystemPromptTemplate(philosopher_response_resource).createMessage(
        Map.of("philosopher_name", state.getPN(), "philosopher_perspective", state.getPP(), "philosopher_style",
                state.getPS(), "summary", state.getSummary(), "messages", state.messages()));
        List<Message> promptMessages = new java.util.ArrayList<>();
        promptMessages.add(message);
        promptMessages.addAll(state.messages());

        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
            .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(this.vectorStore).build())
            .build();

        ChatResponse chatResponse =  ChatClient.builder(this.openAiChatModel)
        .defaultAdvisors(ragAdvisor)
			.build()
			.prompt(new Prompt(promptMessages))
            // Apply a filter expression to the advisor at request time
			.advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "philosopher_id == 'posapiano'"))
            .tools(retrievePhilosopherContext)
			.call()
			.chatResponse();        
        log.info("Philosopher Response: {}", chatResponse.getResult().getOutput().getText());
        assertThat(chatResponse).isNotNull();
    }

    private void addToVectoreStore(Resource pdfResource) throws Exception {
        log.info("Loading PDF file... {}", pdfResource.getFilename());
        var config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(
                        new ExtractedTextFormatter.Builder()
                                .build())
                .build();
        var pdfReader = new PagePdfDocumentReader(pdfResource, config);
        var textSplitter = new TokenTextSplitter();
        List<Document> documents = textSplitter.apply(pdfReader.get()).stream().map(chunk -> {
            return new Document(chunk.getId(), chunk.getText(), Map.of("philosopher_id", "posapiano", "philosopher_name", "Pasquale Rizzi", "source", pdfResource.getFilename()));
        }).toList();
        log.info("created {} documents", documents.size());
        vectorStore.accept(documents);
    }

}