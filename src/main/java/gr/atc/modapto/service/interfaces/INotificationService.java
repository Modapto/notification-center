package gr.atc.modapto.service.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import gr.atc.modapto.dto.NotificationDto;

public interface INotificationService {
    String storeNotification(NotificationDto eventNotification);

    Page<NotificationDto> retrieveAllNotifications(Pageable pageable);

    Page<NotificationDto> retrieveAllNotificationsPerUserId(String userId, Pageable pageable);

    List<NotificationDto> retrieveUnreadNotificationsPerUserId(String userId);

    NotificationDto retrieveNotificationById(String notificationId);

    List<String> retrieveUserIdsPerPilot(String pilot);

    List<String> retrieveUserIdsPerRoles(List<String> roles);

    void updateNotificationStatus(String notificationId);

    void deleteNotificiationById(String notificationId);
}
