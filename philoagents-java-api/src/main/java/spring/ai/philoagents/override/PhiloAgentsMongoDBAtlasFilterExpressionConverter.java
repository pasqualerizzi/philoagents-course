package spring.ai.philoagents.override;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasFilterExpressionConverter;

public class PhiloAgentsMongoDBAtlasFilterExpressionConverter extends MongoDBAtlasFilterExpressionConverter {

    public PhiloAgentsMongoDBAtlasFilterExpressionConverter() {
        super();
    }

    @Override
    protected void doKey(Filter.Key filterKey, StringBuilder context) {
        var identifier = (hasOuterQuotes(filterKey.key())) ? removeOuterQuotes(filterKey.key()) : filterKey.key();
        context.append("\"" + identifier + "\"");
    }

}
