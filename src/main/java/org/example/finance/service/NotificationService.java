package org.example.finance.service;

import org.example.finance.model.FinanceNotification;
import org.example.finance.repo.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository n) {
        notifications = n;
    }

    public List<FinanceNotification> list(String owner) {
        return notifications.findByOwnerOrderByCreatedAtDesc(owner);
    }

    public List<FinanceNotification> unread(String owner) {
        return notifications.findByOwnerAndReadFalseOrderByCreatedAtDesc(owner);
    }

    @Transactional
    public FinanceNotification markRead(String owner, Long id) {
        var n = get(owner, id);
        n.read = true;
        return n;
    }

    @Transactional
    public void markAllRead(String owner) {
        unread(owner).forEach(n -> n.read = true);
    }

    @Transactional
    public void delete(String owner, Long id) {
        notifications.delete(get(owner, id));
    }

    private FinanceNotification get(String owner, Long id) {
        return notifications.findByIdAndOwner(id, owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }
}
