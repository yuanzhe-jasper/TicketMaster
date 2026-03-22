package org.example.repository;

import org.example.model.PagedResult;
import org.example.model.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    void save(Ticket ticket);
    Optional<Ticket> findById(String id);
    List<Ticket> findByEventId(String eventId);
    PagedResult<Ticket> findByEventId(String eventId, int limit, String nextToken);
    void deleteById(String id);
    boolean reserveTicket(String id);
    void releaseTicket(String id);
}