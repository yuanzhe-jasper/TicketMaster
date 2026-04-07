package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.repository.DynamoDbNotificationRepository;
import org.example.service.NotificationService;
import org.example.util.Constants;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class ProcessOrderEventHandler implements RequestHandler<SQSEvent, Void> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationService notificationService;

    public ProcessOrderEventHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String notificationsTable = System.getenv("NOTIFICATIONS_TABLE");
        this.notificationService = new NotificationService(
                new DynamoDbNotificationRepository(dynamoDbClient, notificationsTable)
        );
    }

    ProcessOrderEventHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSMessage message : event.getRecords()) {
            try {
                Map<String, Object> body = objectMapper.readValue(message.getBody(), Map.class);

                String eventType = (String) body.get("eventType");
                String userId = (String) body.get("userId");
                String orderId = (String) body.get("orderId");

                String type;
                String notificationMessage;

                if (Constants.EVENT_TYPE_ORDER_CREATED.equals(eventType)) {
                    type = Constants.NOTIFICATION_TYPE_ORDER_CREATED;
                    notificationMessage = "Your order " + orderId + " has been confirmed.";
                } else if (Constants.EVENT_TYPE_ORDER_CANCELLED.equals(eventType)) {
                    type = Constants.NOTIFICATION_TYPE_ORDER_CANCELLED;
                    notificationMessage = "Your order " + orderId + " has been cancelled.";
                } else {
                    context.getLogger().log("Unknown event type: " + eventType);
                    continue;
                }

                notificationService.createNotification(userId, type, notificationMessage, orderId);
                context.getLogger().log("Created notification for user " + userId + ": " + type);

            } catch (Exception e) {
                context.getLogger().log("Failed to process message: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}