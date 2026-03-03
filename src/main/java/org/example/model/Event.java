package org.example.model;

import lombok.Data;

@Data
public class Event {
    private String id;
    private String name;
    private String venue;
    private String date;
    private int availableTickets;
}
