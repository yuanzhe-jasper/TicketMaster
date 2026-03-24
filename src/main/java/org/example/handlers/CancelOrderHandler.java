package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exception.OrderNotFoundException;
import org.example.model.Order;
import org.example.repository.DynamoDbOrderRepository;
import org.example.repository.DynamoDbTicketRepository;
import org.example.service.OrderService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

public class CancelOrderHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderService orderService;
    private final SqsClient sqsClient;
    private final String orderEventsQueueUrl;

    public CancelOrderHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String ordersTable = System.getenv("ORDERS_TABLE");
        String ticketsTable = System.getenv("TICKETS_TABLE");
        this.orderService = new OrderService(
                new DynamoDbOrderRepository(dynamoDbClient, ordersTable, ticketsTable),
                new DynamoDbTicketRepository(dynamoDbClient, ticketsTable)
        );
        this.sqsClient = SqsClient.create();
        this.orderEventsQueueUrl = System.getenv("ORDER_EVENTS_QUEUE_URL");
    }

    CancelOrderHandler(OrderService orderService, SqsClient sqsClient, String orderEventsQueueUrl) {
        this.orderService = orderService;
        this.sqsClient = sqsClient;
        this.orderEventsQueueUrl = orderEventsQueueUrl;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            String id = request.getPathParameters().get("id");
            Order order = orderService.cancelOrder(id);

            publishOrderEvent(order, "order-cancelled");

            context.getLogger().log("Cancelled order: " + id);
            return response(200, objectMapper.writeValueAsString(order));

        } catch (OrderNotFoundException e) {
            return response(404, "{\"message\":\"" + e.getMessage() + "\"}");
        } catch (IllegalStateException e) {
            return response(409, "{\"message\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return response(500, "{\"message\":\"Internal Server Error\"}");
        }
    }

    private void publishOrderEvent(Order order, String eventType) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "orderId", order.getId(),
                    "userId", order.getUserId(),
                    "eventId", order.getEventId(),
                    "totalPrice", order.getTotalPrice(),
                    "status", order.getStatus()
            );
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(orderEventsQueueUrl)
                    .messageBody(objectMapper.writeValueAsString(event))
                    .build());
        } catch (Exception e) {
            // log but don't fail the cancellation
        }
    }

    private APIGatewayProxyResponseEvent response(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(CORS_HEADERS)
                .withBody(body);
    }
}