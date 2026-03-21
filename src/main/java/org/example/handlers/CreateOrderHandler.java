package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exception.TicketAlreadyBookedException;
import org.example.exception.TicketNotFoundException;
import org.example.model.Order;
import org.example.repository.DynamoDbOrderRepository;
import org.example.repository.DynamoDbTicketRepository;
import org.example.service.OrderService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class CreateOrderHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderService orderService;

    public CreateOrderHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String ordersTable = System.getenv("ORDERS_TABLE");
        String ticketsTable = System.getenv("TICKETS_TABLE");
        this.orderService = new OrderService(
                new DynamoDbOrderRepository(dynamoDbClient, ordersTable, ticketsTable),
                new DynamoDbTicketRepository(dynamoDbClient, ticketsTable)
        );
    }

    CreateOrderHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            CreateOrderRequest orderRequest = objectMapper.readValue(request.getBody(), CreateOrderRequest.class);

            if (orderRequest.userId == null || orderRequest.eventId == null
                    || orderRequest.ticketIds == null || orderRequest.ticketIds.isEmpty()) {
                return response(400, "{\"message\":\"userId, eventId, and ticketIds are required\"}");
            }

            Order order = orderService.createOrder(
                    orderRequest.userId, orderRequest.eventId, orderRequest.ticketIds);
            context.getLogger().log("Created order: " + order.getId());
            return response(201, objectMapper.writeValueAsString(order));

        } catch (TicketNotFoundException e) {
            return response(404, "{\"message\":\"" + e.getMessage() + "\"}");
        } catch (TicketAlreadyBookedException e) {
            return response(409, "{\"message\":\"" + e.getMessage() + "\"}");
        } catch (IllegalArgumentException e) {
            return response(400, "{\"message\":\"" + e.getMessage() + "\"}");
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

    static class CreateOrderRequest {
        public String userId;
        public String eventId;
        public java.util.List<String> ticketIds;
    }
}