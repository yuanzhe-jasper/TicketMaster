package org.example.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@DynamoDbBean
public class Event {
    private String id;
    private String name;
    private String venue;
    private String date;
    private int availableTickets;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}