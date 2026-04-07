package org.example.service;

import org.example.model.Notification;
import org.example.model.PagedResult;
import org.example.repository.NotificationRepository;

import java.time.Instant;
import java.util.UUID;

public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public void createNotification(String userId, String type, String message, String orderId) {
        String notificationId = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSortKey(createdAt + "#" + notificationId);
        notification.setNotificationId(notificationId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setOrderId(orderId);
        notification.setRead(false);
        notification.setCreatedAt(createdAt);

        repository.save(notification);
    }

    public PagedResult<Notification> getNotifications(String userId, int limit, String nextToken) {
        return repository.findByUserId(userId, limit, nextToken);
    }
}