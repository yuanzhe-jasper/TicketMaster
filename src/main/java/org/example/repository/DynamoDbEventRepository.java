package org.example.repository;

import org.example.model.Event;
import org.example.model.PagedResult;
import org.example.model.SearchResult;
import org.example.util.PaginationUtil;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DynamoDbEventRepository implements EventRepository {

    private final DynamoDbTable<Event> table;

    public DynamoDbEventRepository(DynamoDbClient dynamoDbClient, String tableName) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Event.class));
    }

    @Override
    public void save(Event event) {
        table.putItem(event);
    }

    @Override
    public Optional<Event> findById(String id) {
        Event event = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(event);
    }

    @Override
    public List<Event> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public PagedResult<Event> findAll(int limit, String nextToken) {
        ScanEnhancedRequest.Builder requestBuilder = ScanEnhancedRequest.builder().limit(limit);

        Map<String, AttributeValue> exclusiveStartKey = PaginationUtil.decodeToken(nextToken);
        if (exclusiveStartKey != null) {
            requestBuilder.exclusiveStartKey(exclusiveStartKey);
        }

        Page<Event> page = table.scan(requestBuilder.build()).iterator().next();
        String newToken = PaginationUtil.encodeToken(page.lastEvaluatedKey());
        return new PagedResult<>(page.items(), newToken);
    }

    @Override
    public SearchResult<Event> findAll(int page, int size) {
        Map<String, AttributeValue> lastKey = null;
        List<Event> items = Collections.emptyList();

        for (int i = 0; i <= page; i++) {
            ScanEnhancedRequest.Builder builder = ScanEnhancedRequest.builder().limit(size);
            if (lastKey != null) {
                builder.exclusiveStartKey(lastKey);
            }
            Page<Event> result = table.scan(builder.build()).iterator().next();
            items = result.items();
            lastKey = result.lastEvaluatedKey();

            if (lastKey == null && i < page) {
                return new SearchResult<>(Collections.emptyList(), page, size, false);
            }
        }
        return new SearchResult<>(items, page, size, lastKey != null);
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }
}