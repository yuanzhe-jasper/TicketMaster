package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.example.exception.TicketNotFoundException;
import org.example.repository.DynamoDbTicketRepository;
import org.example.service.TicketService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class DeleteTicketHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final TicketService ticketService;

    public DeleteTicketHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        String tableName = System.getenv("TICKETS_TABLE");
        this.ticketService = new TicketService(new DynamoDbTicketRepository(dynamoDbClient, tableName));
    }

    DeleteTicketHandler(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {
        try {
            String id = request.getPathParameters().get("id");
            ticketService.deleteTicket(id);
            context.getLogger().log("Deleted ticket: " + id);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(204)
                    .withHeaders(Map.of("Access-Control-Allow-Origin", "*"));

        } catch (TicketNotFoundException e) {
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