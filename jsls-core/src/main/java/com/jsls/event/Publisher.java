package com.jsls.event;

public interface Publisher {
    <E extends AbstractEvent> void publishEvent(E event, String topic);
}
