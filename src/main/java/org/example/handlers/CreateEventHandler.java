package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Event;
import org.example.repository.DynamoDbEventRepository;
import org.example.service.EventService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class CreateEventHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventService eventService;

    public CreateEventHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String tableName = System.getenv("EVENTS_TABLE");
        this.eventService = new EventService(new DynamoDbEventRepository(dynamoDbClient, tableName));
    }

    CreateEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            Event event = objectMapper.readValue(request.getBody(), Event.class);

            if (event.getName() == null || event.getVenue() == null || event.getDate() == null) {
                return response(400, "{\"message\":\"name, venue, and date are required\"}");
            }

            Event created = eventService.createEvent(event);
            context.getLogger().log("Created event: " + created.getId());
            return response(201, objectMapper.writeValueAsString(created));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return response(500, "{\"message\":\"Internal Server Error\"}");
        }
    }

    private APIGatewayProxyResponseEvent response(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(CORS_HEADERS)
                .withBody(body);
    }
}