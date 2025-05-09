package gr.atc.modapto.repository;

import gr.atc.modapto.config.SetupTestContainersEnvironment;
import gr.atc.modapto.enums.NotificationStatus;
import gr.atc.modapto.enums.NotificationType;
import gr.atc.modapto.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles(profiles = "test")
class NotificationRepositoryTests extends SetupTestContainersEnvironment {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static final String INDEX_NAME = "notifications";
    private static final String USER_ID_1 = "user1";
    private static final String USER_ID_2 = "user2";
    private static final String STATUS_READ = "Read";

    @BeforeEach
    void setup() {
        // Clear the index before each test
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).delete();
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).create();

        // Insert test data
        List<Notification> notifications = createTestNotifications();
        insertNotifications(notifications);

        // Refresh the index to make sure all data is searchable
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
    }

    private List<Notification> createTestNotifications() {
        List<Notification> notifications = new ArrayList<>();

        // Notifications for user1
        notifications.add(createNotification(USER_ID_1, "ASSIGNMENT_1", null, "MODULE_1", NotificationStatus.READ.toString(), NotificationType.ASSIGNMENT.toString()));
        notifications.add(createNotification(USER_ID_1, "ASSIGNMENT_2", null, "MODULE_2", NotificationStatus.UNREAD.toString(), NotificationType.ASSIGNMENT.toString()));
        notifications.add(createNotification(USER_ID_1, null, "EVENT_1", "MODULE_1", NotificationStatus.READ.toString(), NotificationType.EVENT.toString()));

        // Notifications for user2
        notifications.add(createNotification(USER_ID_2, "ASSIGNMENT_3", null, "MODULE_1", NotificationStatus.READ.toString(), NotificationType.ASSIGNMENT.toString()));
        notifications.add(createNotification(USER_ID_2, null, "EVENT_2","MODULE_3", NotificationStatus.UNREAD.toString(), NotificationType.EVENT.toString()));

        return notifications;
    }

    private Notification createNotification(String userId, String relatedAssignment, String relatedEvent, String module, String status, String type) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setRelatedAssignment(relatedAssignment);
        notification.setRelatedEvent(relatedEvent);
        notification.setProductionModule(module);
        notification.setNotificationStatus(status);
        notification.setNotificationType(type);
        notification.setTimestamp(LocalDateTime.now());
        return notification;
    }

    private void insertNotifications(List<Notification> notifications) {
        for (Notification notification : notifications) {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(notification.getId())
                    .withObject(notification)
                    .build();
            elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX_NAME));
        }
    }

    @DisplayName("Find notifications by user ID")
    @Test
    void givenPaginationAndUserId_whenFindByUserId_thenReturnNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Notification> result = notificationRepository.findByUserId(USER_ID_1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        result.getContent().forEach(notification ->
                assertEquals(USER_ID_1, notification.getUserId())
        );
    }

    @DisplayName("Find notifications by user ID and notification status")
    @Test
    void givenPaginationUserIdAndStatus_whenFindByUserIdAndNotificationStatus_thenReturnFilteredNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Notification> result = notificationRepository.findByUserIdAndNotificationStatus(USER_ID_1, STATUS_READ, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        result.getContent().forEach(notification -> {
            assertEquals(USER_ID_1, notification.getUserId());
        });
    }

    @DisplayName("Find notifications with pagination")
    @Test
    void givenDifferentPaginationAndUserId_whenFindByUserId_thenReturnCorrectPage() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // When
        Page<Notification> firstPageResult = notificationRepository.findByUserId(USER_ID_1, firstPage);
        Page<Notification> secondPageResult = notificationRepository.findByUserId(USER_ID_1, secondPage);

        // Then
        assertNotNull(firstPageResult);
        assertNotNull(secondPageResult);
        assertEquals(3, firstPageResult.getTotalElements());
        assertEquals(2, firstPageResult.getContent().size());
        assertEquals(1, secondPageResult.getContent().size());
    }

    @DisplayName("Find notifications by non-existent user ID")
    @Test
    void givenPaginationAndNonExistentUserId_whenFindByUserId_thenReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String nonExistentUserId = "nonexistent";

        // When
        Page<Notification> result = notificationRepository.findByUserId(nonExistentUserId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
    }

    @DisplayName("Save and retrieve notification")
    @Test
    void givenNewNotification_whenSave_thenShouldBeRetrievable() {
        // Given
        String userId = USER_ID_1;
        Notification newNotification = createNotification(userId, "New Assignment", null, "MODULE_4", NotificationStatus.UNREAD.toString(), NotificationType.ASSIGNMENT.toString());
        Pageable pageable = PageRequest.of(0, 10);
        long initialCount = notificationRepository.findByUserId(userId, pageable).getTotalElements();

        // When
        notificationRepository.save(newNotification);
        elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
        Page<Notification> result = notificationRepository.findByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(initialCount + 1, result.getTotalElements());
    }

    @DisplayName("Find notifications by notification type : Success")
    @Test
    void givenNotificationType_whenFindByNotificationType_thenReturnNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String type = NotificationType.ASSIGNMENT.toString();

        // When
        Page<Notification> result = notificationRepository.findByNotificationType(type, pageable);

        // Thens
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        result.getContent().forEach(notification ->
                assertEquals(type, notification.getNotificationType())
        );
    }

    @DisplayName("Find notifications by notification type and user ID : Success")
    @Test
    void givenNotificationTypeAndUserId_whenFindByNotificationTypeAndUserId_thenReturnFilteredNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String type = NotificationType.EVENT.toString();

        // When
        Page<Notification> result = notificationRepository.findByNotificationTypeAndUserId(type, USER_ID_1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        result.getContent().forEach(notification -> {
            assertEquals(type, notification.getNotificationType());
            assertEquals(USER_ID_1, notification.getUserId());
        });
    }

}