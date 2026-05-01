package ca.optimusAI.tms.notification.service;

import ca.optimusAI.tms.validation.event.ValidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes real-time notifications over WebSocket (STOMP).
 * Destination: /topic/tenant/{tenantId}/notifications
 * All connected clients subscribed to that topic receive the event immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts the validation event to all subscribers of the tenant topic.
     *
     * @param event the validation event to broadcast
     */
    public void push(ValidationEvent event) {
        String destination = "/topic/tenant/" + event.tenantId() + "/notifications";
        try {
            messagingTemplate.convertAndSend(destination, event);
            log.debug("WebSocket push → {} eventType={} sessionId={}",
                    destination, event.eventType(), event.sessionId());
        } catch (Exception e) {
            log.error("WebSocket push failed for session {} to {}: {}",
                    event.sessionId(), destination, e.getMessage());
        }
    }
}
