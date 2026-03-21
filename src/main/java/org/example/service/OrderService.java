package org.example.service;

import org.example.exception.OrderNotFoundException;
import org.example.exception.TicketAlreadyBookedException;
import org.example.exception.TicketNotFoundException;
import org.example.model.Order;
import org.example.model.Ticket;
import org.example.repository.OrderRepository;
import org.example.repository.TicketRepository;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;

    public OrderService(OrderRepository orderRepository, TicketRepository ticketRepository) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
    }

    public Order createOrder(String userId, String eventId, List<String> ticketIds) {
        // Validate all tickets exist and belong to the event
        double totalPrice = 0;
        for (String ticketId : ticketIds) {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));

            if (!ticket.getEventId().equals(eventId)) {
                throw new IllegalArgumentException("Ticket " + ticketId + " does not belong to event " + eventId);
            }
            totalPrice += ticket.getPrice();
        }

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setEventId(eventId);
        order.setTicketIds(ticketIds);
        order.setTotalPrice(totalPrice);
        order.setStatus("CONFIRMED");
        order.setCreatedAt(Instant.now().toString());

        try {
            // Atomic transaction: reserve all tickets + create order
            orderRepository.createOrderWithTickets(order, ticketIds);
        } catch (TransactionCanceledException e) {
            throw new TicketAlreadyBookedException("One or more tickets are no longer available");
        }

        return order;
    }

    public Order getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order cancelOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Order is already cancelled");
        }

        // Atomic transaction: release all tickets + update order status
        orderRepository.cancelOrderWithTickets(order, order.getTicketIds());

        order.setStatus("CANCELLED");
        return order;
    }
}