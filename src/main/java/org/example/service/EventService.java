package org.example.service;

import org.example.exception.EventNotFoundException;
import org.example.model.Event;
import org.example.model.PagedResult;
import org.example.model.SearchResult;
import org.example.repository.EventRepository;

import java.util.List;
import java.util.UUID;

public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public Event createEvent(Event event) {
        event.setId(UUID.randomUUID().toString());
        repository.save(event);
        return event;
    }

    public Event getEvent(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    public List<Event> getAllEvents() {
        return repository.findAll();
    }

    public PagedResult<Event> getAllEvents(int limit, String nextToken) {
        return repository.findAll(limit, nextToken);
    }

    public SearchResult<Event> searchEvents(int page, int size) {
        return repository.findAll(page, size);
    }

    public Event updateEvent(String id, Event event) {
        repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        event.setId(id);
        repository.save(event);
        return event;
    }

    public void deleteEvent(String id) {
        repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        repository.deleteById(id);
    }
}