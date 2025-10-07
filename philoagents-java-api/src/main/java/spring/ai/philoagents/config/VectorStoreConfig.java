package spring.ai.philoagents.config;

import java.util.List;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SpringAIVectorStoreTypes;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.autoconfigure.MongoDBAtlasVectorStoreProperties;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.CollectionUtils;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import spring.ai.philoagents.override.PhiloAgentsMongoDBAtlasVectorStore;

@Configuration
@ConditionalOnProperty(name = SpringAIVectorStoreTypes.TYPE, havingValue = SpringAIVectorStoreTypes.MONGODB_ATLAS, matchIfMissing = true)
@Slf4j
public class VectorStoreConfig {

    @Primary
    @Bean
    VectorStore getVectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel,
            MongoDBAtlasVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
            BatchingStrategy batchingStrategy) {

        PhiloAgentsMongoDBAtlasVectorStore.Builder builder = PhiloAgentsMongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy);

        PropertyMapper mapper = PropertyMapper.get();
        mapper.from(properties::getCollectionName).whenHasText().to(builder::collectionName);
        mapper.from(properties::getPathName).whenHasText().to(builder::pathName);
        mapper.from(properties::getIndexName).whenHasText().to(builder::vectorIndexName);

        List<String> metadataFields = properties.getMetadataFieldsToFilter();
        if (!CollectionUtils.isEmpty(metadataFields)) {
            builder.metadataFieldsToFilter(metadataFields);
        }

        return builder.build();
    }
}
