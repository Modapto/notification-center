package gr.atc.modapto.service.interfaces;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import gr.atc.modapto.dto.AssignmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import gr.atc.modapto.dto.NotificationDto;

public interface INotificationService {
    String storeNotification(NotificationDto eventNotification);

    Page<NotificationDto> retrieveAllNotifications(Pageable pageable);

    Page<NotificationDto> retrieveAllNotificationsPerUserId(String userId, Pageable pageable);

    List<NotificationDto> retrieveUnreadNotificationsPerUserId(String userId);

    Page<NotificationDto> retrieveAllNotificationsPerNotificationType(String notificationType, Pageable pageable);

    Page<NotificationDto> retrieveAllNotificationsPerNotificationTypeAndUserId(String notificationType, String  userId, Pageable pageable);

    NotificationDto retrieveNotificationById(String notificationId);

    List<String> retrieveUserIdsPerPilot(String pilot);

    List<String> retrieveUserIdsPerRoles(List<String> roles);

    void updateNotificationStatusToRead(String notificationId);

    void deleteNotificationById(String notificationId);

    CompletableFuture<Void> createNotificationAndNotifyUser(AssignmentDto assignmentDto);
}
