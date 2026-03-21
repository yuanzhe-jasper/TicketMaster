package org.example.exception;

public class TicketAlreadyBookedException extends RuntimeException {
    public TicketAlreadyBookedException(String ticketId) {
        super("Ticket already booked: " + ticketId);
    }
}
