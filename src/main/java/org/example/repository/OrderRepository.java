package org.example.repository;

import org.example.model.Order;
import org.example.model.PagedResult;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
    List<Order> findByUserId(String userId);
    PagedResult<Order> findByUserId(String userId, int limit, String nextToken);
    void createOrderWithTickets(Order order, List<String> ticketIds);
    void cancelOrderWithTickets(Order order, List<String> ticketIds);
}
