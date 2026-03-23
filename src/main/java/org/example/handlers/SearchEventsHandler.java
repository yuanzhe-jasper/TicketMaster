package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.repository.DynamoDbEventRepository;
import org.example.service.EventService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class SearchEventsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventService eventService;

    public SearchEventsHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String tableName = System.getenv("EVENTS_TABLE");
        this.eventService = new EventService(new DynamoDbEventRepository(dynamoDbClient, tableName));
    }

    SearchEventsHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();

            int page = 0;
            int size = 20;

            if (queryParams != null) {
                if (queryParams.containsKey("page")) {
                    page = Integer.parseInt(queryParams.get("page"));
                }
                if (queryParams.containsKey("size")) {
                    size = Integer.parseInt(queryParams.get("size"));
                }
            }

            if (page < 0 || size <= 0) {
                return response(400, "{\"message\":\"page must be >= 0 and size must be > 0\"}");
            }

            return response(200, objectMapper.writeValueAsString(eventService.searchEvents(page, size)));

        } catch (NumberFormatException e) {
            return response(400, "{\"message\":\"page and size must be valid integers\"}");
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