package org.example.repository;

import org.example.model.Notification;
import org.example.model.PagedResult;

public interface NotificationRepository {
    void save(Notification notification);
    PagedResult<Notification> findByUserId(String userId, int limit, String nextToken);
}
