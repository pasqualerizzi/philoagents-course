package spring.ai.philoagents.repository;

import java.util.List;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;

public interface PhilosopherSearchRepository {

    void addDocuments(List<Document> docs);

    void deleteDocuments(List<String> ids);

    List<Document> semanticSearchByQuery(SearchRequest searchRequest);

}
