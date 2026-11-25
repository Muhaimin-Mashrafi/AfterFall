package com.afterfall.web;

import com.afterfall.application.SimulationService;
import com.afterfall.events.EventSubscriber;
import com.afterfall.events.SimEvent;
import com.afterfall.narrator.StoryCaption;
import jakarta.annotation.PostConstruct;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * WebSocket broadcaster publishing simulation events and documentary captions
 * to connected STOMP subscribers.
 */
@Component
public class WebSocketBroadcaster implements EventSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimulationService simulationService;

    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate, SimulationService simulationService) {
        this.messagingTemplate = messagingTemplate;
        this.simulationService = simulationService;
    }

    @PostConstruct
    public void init() {
        simulationService.addCaptionListener(this::broadcastCaption);
        simulationService.addEventSubscriber(this);
    }

    @Override
    public String subscriberId() {
        return "websocket-broadcaster";
    }

    @Override
    public void onEvents(List<SimEvent> events) {
        if (events == null || events.isEmpty()) return;
        for (SimEvent event : events) {
            Map<String, Object> payload = Map.of(
                    "id", event.id().value(),
                    "tick", event.tick(),
                    "type", event.eventType(),
                    "severity", event.severity().name(),
                    "summary", event.summary()
            );
            messagingTemplate.convertAndSend("/topic/events", (Object) payload);
        }
    }

    public void broadcastCaption(StoryCaption caption) {
        if (caption == null) return;
        Map<String, Object> payload = Map.of(
                "captionId", caption.captionId(),
                "tick", caption.tick(),
                "text", caption.text(),
                "severity", caption.severity().name()
        );
        messagingTemplate.convertAndSend("/topic/captions", (Object) payload);
    }
}
