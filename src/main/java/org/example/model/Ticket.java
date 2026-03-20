package org.example.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Data
@DynamoDbBean
public class Ticket {
    private String id;
    private String eventId;
    private String section;
    private String row;
    private String seatNumber;
    private double price;
    private String status; // AVAILABLE, RESERVED, SOLD

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "eventId-index")
    public String getEventId() {
        return eventId;
    }
}