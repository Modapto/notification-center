package gr.atc.modapto.service;

import java.util.List;

import gr.atc.modapto.dto.NotificationDto;
import gr.atc.modapto.enums.UserRole;

public interface INotificationService {
    String storeNotification(NotificationDto eventNotification);

    List<NotificationDto> retrieveAllNotifications();

    List<NotificationDto> retrieveNotificationPerUserId(String userId);

    List<NotificationDto> retrieveUnreadNotificationsPerUserId(String userId);

    NotificationDto retrieveNotificationById(String notificationId);

    List<String> retrieveUserIdsPerPilot(String pilot);

    List<String> retrieveUserIdsPerRoles(List<UserRole> roles);
}
