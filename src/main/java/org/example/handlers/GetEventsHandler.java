package org.example.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public class GetEventsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String STUB_BODY = """
            [
              {
                "id": "1",
                "name": "Taylor Swift - Eras Tour",
                "venue": "Madison Square Garden",
                "date": "2026-06-15",
                "availableTickets": 120
              },
              {
                "id": "2",
                "name": "NBA Finals Game 1",
                "venue": "Chase Center",
                "date": "2026-06-10",
                "availableTickets": 300
              },
              {
                "id": "3",
                "name": "Hamilton",
                "venue": "Richard Rodgers Theatre",
                "date": "2026-07-04",
                "availableTickets": 50
              }
            ]
            """;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, Context context) {

        context.getLogger().log("GetEvents invoked");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                ))
                .withBody(STUB_BODY.strip());
    }
}