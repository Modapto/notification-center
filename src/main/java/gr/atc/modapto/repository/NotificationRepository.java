package gr.atc.modapto.repository;

import gr.atc.modapto.model.Notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface NotificationRepository extends ElasticsearchRepository<Notification, String> {

    Page<Notification> findByUserId(String userIds, Pageable pageable);

    Page<Notification> findByUserIdAndNotificationStatus(String userId, String notificationStatus, Pageable pageable);
}
