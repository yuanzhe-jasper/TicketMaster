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

import java.util.Map;

public class GetOrderHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderService orderService;

    public GetOrderHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String ordersTable = System.getenv("ORDERS_TABLE");
        String ticketsTable = System.getenv("TICKETS_TABLE");
        this.orderService = new OrderService(
                new DynamoDbOrderRepository(dynamoDbClient, ordersTable, ticketsTable),
                new DynamoDbTicketRepository(dynamoDbClient, ticketsTable)
        );
    }

    GetOrderHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            String id = request.getPathParameters().get("id");
            Order order = orderService.getOrder(id);
            return response(200, objectMapper.writeValueAsString(order));

        } catch (OrderNotFoundException e) {
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