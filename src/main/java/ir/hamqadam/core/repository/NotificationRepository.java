package ir.hamqadam.core.repository;

import ir.hamqadam.core.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Finds notifications for a specific recipient, ordered by creation date descending.
     * @param recipientUserId The ID of the recipient user.
     * @param pageable Pagination and sorting information.
     * @return A page of notifications.
     */
    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    /**
     * Finds unread notifications for a specific recipient.
     * @param recipientUserId The ID of the recipient user.
     * @param pageable Pagination and sorting information.
     * @return A page of unread notifications.
     */
    Page<Notification> findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    /**
     * Counts unread notifications for a specific recipient.
     * @param recipientUserId The ID of the recipient user.
     * @return The count of unread notifications.
     */
    long countByRecipientUserIdAndReadFalse(String recipientUserId);
}