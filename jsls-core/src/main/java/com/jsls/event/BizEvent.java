package com.jsls.event;

import com.jsls.event.AbstractEvent.Topic;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Topic("${topic.bizEvent}")
public class BizEvent extends AbstractEvent {
    private String customerId;
    private String bizId;

    public BizEvent(String eventName, String scene) {
        super(eventName, scene);
    }

    public static BizEvent of(String scene, String eventName) {
        BizEvent event = new BizEvent(eventName, scene);
        return event;
    }
}
