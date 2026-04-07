package org.example.repository;

import org.example.model.Notification;
import org.example.model.PagedResult;
import org.example.util.PaginationUtil;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class DynamoDbNotificationRepository implements NotificationRepository {

    private final DynamoDbTable<Notification> table;

    public DynamoDbNotificationRepository(DynamoDbClient dynamoDbClient, String tableName) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Notification.class));
    }

    @Override
    public void save(Notification notification) {
        table.putItem(notification);
    }

    @Override
    public PagedResult<Notification> findByUserId(String userId, int limit, String nextToken) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build());

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(condition)
                .scanIndexForward(false) // newest first
                .limit(limit);

        Map<String, AttributeValue> exclusiveStartKey = PaginationUtil.decodeToken(nextToken);
        if (exclusiveStartKey != null) {
            requestBuilder.exclusiveStartKey(exclusiveStartKey);
        }

        Page<Notification> page = table.query(requestBuilder.build()).iterator().next();
        String newToken = PaginationUtil.encodeToken(page.lastEvaluatedKey());
        return new PagedResult<>(page.items(), newToken);
    }
}
