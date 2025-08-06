package spring.ai.philoagents.repository;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

@Repository
public class PhilosopherSearchRepositoryImpl implements PhilosopherSearchRepository {

    private final VectorStore vectorStore;

    public PhilosopherSearchRepositoryImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void addDocuments(List<Document> docs) {
        vectorStore.add(docs);
    }

    @Override
    public void deleteDocuments(List<String> ids) {
        vectorStore.delete(ids);
    }

    @Override
    public List<Document> semanticSearchByQuery(SearchRequest searchRequest) {
        return vectorStore.similaritySearch(searchRequest);
    }

}
