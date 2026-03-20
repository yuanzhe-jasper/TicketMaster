package org.example.repository;

import org.example.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    void save(Event event);
    Optional<Event> findById(String id);
    List<Event> findAll();
    void deleteById(String id);
}