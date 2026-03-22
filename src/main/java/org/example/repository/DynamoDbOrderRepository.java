package org.example.repository;

import org.example.model.Order;
import org.example.model.PagedResult;
import org.example.util.PaginationUtil;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DynamoDbOrderRepository implements OrderRepository {

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbTable<Order> table;
    private final DynamoDbIndex<Order> userIdIndex;
    private final String tableName;
    private final String ticketsTableName;

    public DynamoDbOrderRepository(DynamoDbClient dynamoDbClient, String tableName, String ticketsTableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.ticketsTableName = ticketsTableName;
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Order.class));
        this.userIdIndex = table.index("userId-index");
    }

    @Override
    public void save(Order order) {
        table.putItem(order);
    }

    @Override
    public Optional<Order> findById(String id) {
        Order order = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(order);
    }

    @Override
    public List<Order> findByUserId(String userId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build());
        return userIdIndex.query(condition)
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    @Override
    public PagedResult<Order> findByUserId(String userId, int limit, String nextToken) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build());

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(condition)
                .limit(limit);

        Map<String, AttributeValue> exclusiveStartKey = PaginationUtil.decodeToken(nextToken);
        if (exclusiveStartKey != null) {
            requestBuilder.exclusiveStartKey(exclusiveStartKey);
        }

        Page<Order> page = userIdIndex.query(requestBuilder.build()).iterator().next();
        String newToken = PaginationUtil.encodeToken(page.lastEvaluatedKey());
        return new PagedResult<>(page.items(), newToken);
    }

    @Override
    public void createOrderWithTickets(Order order, List<String> ticketIds) {
        List<TransactWriteItem> actions = new ArrayList<>();

        // Update each ticket: AVAILABLE → SOLD (conditional)
        for (String ticketId : ticketIds) {
            actions.add(TransactWriteItem.builder()
                    .update(Update.builder()
                            .tableName(ticketsTableName)
                            .key(Map.of("id", AttributeValue.fromS(ticketId)))
                            .updateExpression("SET #s = :sold")
                            .conditionExpression("#s = :available")
                            .expressionAttributeNames(Map.of("#s", "status"))
                            .expressionAttributeValues(Map.of(
                                    ":sold", AttributeValue.fromS("SOLD"),
                                    ":available", AttributeValue.fromS("AVAILABLE")
                            ))
                            .build())
                    .build());
        }

        // Create the order
        Map<String, AttributeValue> orderItem = new HashMap<>();
        orderItem.put("id", AttributeValue.fromS(order.getId()));
        orderItem.put("userId", AttributeValue.fromS(order.getUserId()));
        orderItem.put("eventId", AttributeValue.fromS(order.getEventId()));
        orderItem.put("ticketIds", AttributeValue.fromL(
                order.getTicketIds().stream()
                        .map(AttributeValue::fromS)
                        .toList()));
        orderItem.put("totalPrice", AttributeValue.fromN(String.valueOf(order.getTotalPrice())));
        orderItem.put("status", AttributeValue.fromS(order.getStatus()));
        orderItem.put("createdAt", AttributeValue.fromS(order.getCreatedAt()));

        actions.add(TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(tableName)
                        .item(orderItem)
                        .build())
                .build());

        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(actions)
                .build());
    }

    @Override
    public void cancelOrderWithTickets(Order order, List<String> ticketIds) {
        List<TransactWriteItem> actions = new ArrayList<>();

        // Release each ticket: SOLD → AVAILABLE
        for (String ticketId : ticketIds) {
            actions.add(TransactWriteItem.builder()
                    .update(Update.builder()
                            .tableName(ticketsTableName)
                            .key(Map.of("id", AttributeValue.fromS(ticketId)))
                            .updateExpression("SET #s = :available")
                            .expressionAttributeNames(Map.of("#s", "status"))
                            .expressionAttributeValues(Map.of(
                                    ":available", AttributeValue.fromS("AVAILABLE")
                            ))
                            .build())
                    .build());
        }

        // Update order status to CANCELLED
        actions.add(TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName(tableName)
                        .key(Map.of("id", AttributeValue.fromS(order.getId())))
                        .updateExpression("SET #s = :cancelled")
                        .expressionAttributeNames(Map.of("#s", "status"))
                        .expressionAttributeValues(Map.of(
                                ":cancelled", AttributeValue.fromS("CANCELLED")
                        ))
                        .build())
                .build());

        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(actions)
                .build());
    }
}