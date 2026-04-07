package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.repository.DynamoDbNotificationRepository;
import org.example.service.NotificationService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class GetNotificationsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationService notificationService;

    public GetNotificationsHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String notificationsTable = System.getenv("NOTIFICATIONS_TABLE");
        this.notificationService = new NotificationService(
                new DynamoDbNotificationRepository(dynamoDbClient, notificationsTable)
        );
    }

    GetNotificationsHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            String userId = request.getPathParameters().get("userId");
            Map<String, String> queryParams = request.getQueryStringParameters();

            int limit = 20;
            String nextToken = null;

            if (queryParams != null) {
                if (queryParams.containsKey("limit")) {
                    limit = Integer.parseInt(queryParams.get("limit"));
                }
                nextToken = queryParams.getOrDefault("nextToken", null);
            }

            return response(200, objectMapper.writeValueAsString(
                    notificationService.getNotifications(userId, limit, nextToken)));

        } catch (NumberFormatException e) {
            return response(400, "{\"message\":\"limit must be a valid integer\"}");
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
