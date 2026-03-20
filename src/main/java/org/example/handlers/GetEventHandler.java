package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exception.EventNotFoundException;
import org.example.model.Event;
import org.example.repository.DynamoDbEventRepository;
import org.example.service.EventService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class GetEventHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventService eventService;

    public GetEventHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String tableName = System.getenv("EVENTS_TABLE");
        this.eventService = new EventService(new DynamoDbEventRepository(dynamoDbClient, tableName));
    }

    GetEventHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            String id = request.getPathParameters().get("id");
            Event event = eventService.getEvent(id);
            return response(200, objectMapper.writeValueAsString(event));

        } catch (EventNotFoundException e) {
            return response(404, "{\"message\":\"" + e.getMessage() + "\"}");
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