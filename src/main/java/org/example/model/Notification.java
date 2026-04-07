package org.example.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@DynamoDbBean
public class Notification {
    private String userId;
    private String sortKey;  // createdAt#notificationId
    private String notificationId;
    private String type;     // ORDER_CREATED, ORDER_CANCELLED
    private String message;
    private String orderId;
    private boolean read;
    private String createdAt;

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    @DynamoDbSortKey
    public String getSortKey() {
        return sortKey;
    }
}
