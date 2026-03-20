package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Event;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetEventsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tableName = System.getenv("EVENTS_TABLE");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            ScanResponse response = dynamoDb.scan(ScanRequest.builder()
                    .tableName(tableName)
                    .build());

            List<Event> events = new ArrayList<>();
            for (Map<String, AttributeValue> item : response.items()) {
                Event event = new Event();
                event.setId(item.get("id").s());
                event.setName(item.get("name").s());
                event.setVenue(item.get("venue").s());
                event.setDate(item.get("date").s());
                event.setAvailableTickets(Integer.parseInt(item.get("availableTickets").n()));
                events.add(event);
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*"
                    ))
                    .withBody(objectMapper.writeValueAsString(events));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"message\":\"Internal Server Error\"}");
        }
    }
}
