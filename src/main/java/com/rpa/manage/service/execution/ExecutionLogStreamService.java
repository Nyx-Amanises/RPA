package com.rpa.manage.service.execution;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ExecutionLogStreamService {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private static final int MAX_REPLAY_EVENTS = 200;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArrayList<Map<String, Object>>> replayEvents = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long executionId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.computeIfAbsent(executionId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(executionId, emitter));
        emitter.onTimeout(() -> removeEmitter(executionId, emitter));
        emitter.onError(ignored -> removeEmitter(executionId, emitter));

        send(emitter, "connected", Map.of(
                "eventType", "connected",
                "executionId", executionId,
                "eventTime", LocalDateTime.now()
        ));
        for (Map<String, Object> event : replayEvents.getOrDefault(executionId, new CopyOnWriteArrayList<>())) {
            send(emitter, String.valueOf(event.getOrDefault("eventType", "message")), event);
        }
        return emitter;
    }

    public void emit(Long executionId, String eventType, Map<String, Object> payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.putAll(payload);
        event.put("eventType", eventType);
        event.put("executionId", executionId);
        event.put("eventTime", LocalDateTime.now());

        CopyOnWriteArrayList<Map<String, Object>> replay = replayEvents.computeIfAbsent(executionId, ignored -> new CopyOnWriteArrayList<>());
        replay.add(event);
        while (replay.size() > MAX_REPLAY_EVENTS) {
            replay.remove(0);
        }

        for (SseEmitter emitter : emitters.getOrDefault(executionId, new CopyOnWriteArrayList<>())) {
            send(emitter, eventType, event);
        }
    }

    public void complete(Long executionId) {
        List<SseEmitter> currentEmitters = new ArrayList<>(emitters.getOrDefault(executionId, new CopyOnWriteArrayList<>()));
        emitters.remove(executionId);
        for (SseEmitter emitter : currentEmitters) {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // client already disconnected
            }
        }
    }

    private void send(SseEmitter emitter, String eventType, Map<String, Object> event) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventType)
                            .data(event, MediaType.APPLICATION_JSON)
            );
        } catch (IOException | IllegalStateException ex) {
            try {
                emitter.completeWithError(ex);
            } catch (IllegalStateException ignored) {
                // client already disconnected
            }
        }
    }

    private void removeEmitter(Long executionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> currentEmitters = emitters.get(executionId);
        if (currentEmitters != null) {
            currentEmitters.remove(emitter);
        }
    }
}
