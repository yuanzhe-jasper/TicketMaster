package org.example.repository;

import org.example.model.Event;
import org.example.model.PagedResult;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    void save(Event event);
    Optional<Event> findById(String id);
    List<Event> findAll();
    PagedResult<Event> findAll(int limit, String nextToken);
    void deleteById(String id);
}