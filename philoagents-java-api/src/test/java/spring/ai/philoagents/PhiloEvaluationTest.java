package spring.ai.philoagents;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PhiloTestConfiguration.class)
@ContextConfiguration(classes=TestApplication.class)
//@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*")
public class PhiloEvaluationTest{

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient contentEvaluator;

	@Autowired
	private ChatModel openAiChatModel;

    @Test
    public void testQuestionAnswerAccuracy() throws Exception {
        String question = "Hi my name is Sophia, what is your perspective on the theory of forms?";
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                 .searchRequest(
                    SearchRequest.builder().similarityThreshold(0.8d).topK(6)
                    .filterExpression(new FilterExpressionBuilder()
                    .eq("philosopher_name", "plato").build())
                .build())
        .build();
		ChatResponse chatResponse = ChatClient.builder(this.openAiChatModel).defaultAdvisors(questionAnswerAdvisor)
			.build()
			.prompt().user(question)
			.call()
			.chatResponse();        
		assertThat(chatResponse).isNotNull();
        // Evaluate if the answer is relevant and related to the question
        evaluateRelevancyForQA(question, chatResponse);
    }    

	@Test
	void ragWithRequestFilter() {
		String question = "You were influenced by pre-Socratic thinkers such as Pythagoras and Heraclitus, can you tell me more about how their ideas shaped your philosophy?";

		RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
			.documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(this.vectorStore).build())
			.build();

		ChatResponse chatResponse = ChatClient.builder(this.openAiChatModel)
        .defaultAdvisors(ragAdvisor)
			.build()
			.prompt().user(question)
            // Apply a filter expression to the advisor at request time
			.advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "philosopher_name == 'plato'"))
			.call()
			.chatResponse();

		assertThat(chatResponse).isNotNull();
		// No documents retrieved since the filter expression matches none of the
		// documents in the vector store.
		assertThat((String) chatResponse.getResult().getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT))
			.isNull();

        evaluateRelevancy(question, chatResponse);
	}  

	@Test
	void ragWithRequestFilterCorrectness() {
		String question = "Hi my name is Sophia, can you tell me about your experiences in the Dutch States Army?";

		RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
			.documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(this.vectorStore).build())
			.build();

		ChatResponse chatResponse = ChatClient.builder(this.openAiChatModel)
        .defaultAdvisors(ragAdvisor)
			.build()
			.prompt().user(question)
            // Apply a filter expression to the advisor at request time
			.advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "philosopher_name == 'descartes'"))
			.call()
			.chatResponse();

		assertThat(chatResponse).isNotNull();
		// No documents retrieved since the filter expression matches none of the
		// documents in the vector store.
		assertThat((String) chatResponse.getResult().getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT))
			.isNull();

        evaluateCorrectness(question, chatResponse);
	}    
    
	private void evaluateRelevancyForQA(String question, ChatResponse chatResponse) {
		EvaluationRequest evaluationRequest = new EvaluationRequest(question,
				chatResponse.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS),
				chatResponse.getResult().getOutput().getText());
		RelevancyEvaluator evaluator = new RelevancyEvaluator(ChatClient.builder(this.openAiChatModel));
		EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);
		assertThat(evaluationResponse.isPass()).isTrue();
	}     
    
	private void evaluateRelevancy(String question, ChatResponse chatResponse) {
		EvaluationRequest evaluationRequest = new EvaluationRequest(question,
				chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT),
				chatResponse.getResult().getOutput().getText());
		RelevancyEvaluator evaluator = new RelevancyEvaluator(this.contentEvaluator.mutate());
		EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);
		assertThat(evaluationResponse.isPass()).isTrue();
	}   

	private void evaluateCorrectness(String question, ChatResponse chatResponse) {
		EvaluationRequest evaluationRequest = new EvaluationRequest(question,
				chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT),
				chatResponse.getResult().getOutput().getText());
		FactCheckingEvaluator evaluator = new FactCheckingEvaluator(this.contentEvaluator.mutate());
		EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);
		assertThat(evaluationResponse.isPass()).isTrue();
	}       
}
