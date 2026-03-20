package org.example.repository;

import org.example.model.Event;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
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
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }
}