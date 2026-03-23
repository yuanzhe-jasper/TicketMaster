package org.example.repository;

import org.example.model.Event;
import org.example.model.PagedResult;
import org.example.model.SearchResult;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    void save(Event event);
    Optional<Event> findById(String id);
    List<Event> findAll();
    PagedResult<Event> findAll(int limit, String nextToken);
    SearchResult<Event> findAll(int page, int size);
    void deleteById(String id);
}