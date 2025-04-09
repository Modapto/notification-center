package gr.atc.modapto.service;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify all users through WebSocket
     * 
     * @param message : String message
     * @param topicName : Topic Name for specific user roles
     */
    public void notifyRolesWebSocket(String message, String topicName){
        try {
            String websocketTopic = "/events/" + topicName.toLowerCase();
            log.info("Notifying websocket topic: {}", websocketTopic);
            messagingTemplate.convertAndSend(websocketTopic, message);
        } catch (MessagingException e) {
            log.error("Error in sending data via websockets - {}", e.getMessage());
        }
    }

    /**
     * Notify specific user through WebSocket
     * 
     * @param userId : User ID of user that will connect to specific topics
     * @param message : String message
     */
    public void notifyUserWebSocket(String userId, String message) {
        try {
            String websocketTopic = "/user/" + userId + "/queue/notifications";
            log.info("Notifying user: {} on websocket topic: {}", userId, websocketTopic);
            messagingTemplate.convertAndSendToUser(userId, "/queue/events", message);
        } catch (MessagingException e) {
            log.error("Error in sending data to user via websockets - {}", e.getMessage());
        }
    }
}

