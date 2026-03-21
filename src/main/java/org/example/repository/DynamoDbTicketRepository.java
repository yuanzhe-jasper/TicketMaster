package org.example.repository;

import org.example.model.Ticket;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DynamoDbTicketRepository implements TicketRepository {

    private final DynamoDbTable<Ticket> table;
    private final DynamoDbIndex<Ticket> eventIdIndex;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbTicketRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Ticket.class));
        this.eventIdIndex = table.index("eventId-index");
    }

    @Override
    public void save(Ticket ticket) {
        table.putItem(ticket);
    }

    @Override
    public Optional<Ticket> findById(String id) {
        Ticket ticket = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(ticket);
    }

    @Override
    public List<Ticket> findByEventId(String eventId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(eventId).build());
        return eventIdIndex.query(condition)
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }

    @Override
    public boolean reserveTicket(String id) {
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("id", AttributeValue.fromS(id)))
                    .updateExpression("SET #s = :sold")
                    .conditionExpression("#s = :available")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(
                            ":sold", AttributeValue.fromS("SOLD"),
                            ":available", AttributeValue.fromS("AVAILABLE")
                    ))
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    @Override
    public void releaseTicket(String id) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .updateExpression("SET #s = :available")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(
                        ":available", AttributeValue.fromS("AVAILABLE")
                ))
                .build());
    }
}