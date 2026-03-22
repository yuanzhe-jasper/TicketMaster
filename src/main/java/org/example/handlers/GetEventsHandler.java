package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.PagedResult;
import org.example.repository.DynamoDbEventRepository;
import org.example.service.EventService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class GetEventsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventService eventService;

    public GetEventsHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String tableName = System.getenv("EVENTS_TABLE");
        this.eventService = new EventService(new DynamoDbEventRepository(dynamoDbClient, tableName));
    }

    GetEventsHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams != null && queryParams.containsKey("limit")) {
                int limit = Integer.parseInt(queryParams.get("limit"));
                String nextToken = queryParams.getOrDefault("nextToken", null);
                PagedResult<?> result = eventService.getAllEvents(limit, nextToken);
                return response(200, objectMapper.writeValueAsString(result));
            }
            return response(200, objectMapper.writeValueAsString(eventService.getAllEvents()));

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