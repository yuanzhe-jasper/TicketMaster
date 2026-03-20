package org.example.repository;

import org.example.model.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    void save(Ticket ticket);
    Optional<Ticket> findById(String id);
    List<Ticket> findByEventId(String eventId);
    void deleteById(String id);
}