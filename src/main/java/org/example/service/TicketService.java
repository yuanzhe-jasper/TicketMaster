package org.example.service;

import org.example.exception.TicketNotFoundException;
import org.example.model.PagedResult;
import org.example.model.Ticket;
import org.example.repository.TicketRepository;

import java.util.List;
import java.util.UUID;

public class TicketService {

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    public Ticket createTicket(String eventId, Ticket ticket) {
        ticket.setId(UUID.randomUUID().toString());
        ticket.setEventId(eventId);
        if (ticket.getStatus() == null) {
            ticket.setStatus("AVAILABLE");
        }
        repository.save(ticket);
        return ticket;
    }

    public Ticket getTicket(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    public List<Ticket> getTicketsByEvent(String eventId) {
        return repository.findByEventId(eventId);
    }

    public PagedResult<Ticket> getTicketsByEvent(String eventId, int limit, String nextToken) {
        return repository.findByEventId(eventId, limit, nextToken);
    }

    public Ticket updateTicket(String id, Ticket ticket) {
        Ticket existing = repository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticket.setId(id);
        ticket.setEventId(existing.getEventId());
        repository.save(ticket);
        return ticket;
    }

    public void deleteTicket(String id) {
        repository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        repository.deleteById(id);
    }
}