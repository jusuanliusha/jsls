package com.jsls.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.StringUtils;

import com.jsls.util.SpringContextHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractEvent extends ApplicationEvent {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractEvent.class);
    private static final Map<Class<?>, String> defaultTopicMap = new ConcurrentHashMap<>();
    private String eventName;
    private String scene;
    private Map<String, Object> factors = new HashMap<String, Object>();

    public AbstractEvent(String eventName, String scene) {
        super((String) SpringContextHolder.resolveExpression("${spring.application.name}"));
        this.eventName = eventName;
        this.scene = scene;
    }

    @SuppressWarnings("unchecked")
    public <D> D getFactor(String key) {
        if (factors != null) {
            return (D) factors.get(key);
        }
        return null;
    }

    public void addFactor(String key, Object value) {
        if (factors == null) {
            factors = new HashMap<String, Object>();
        }
        factors.put(key, value);
    }

    /**
     * 发送事件
     */
    public void emit(boolean async) {
        emit(useDefaultTopic(getClass()), async);
    }

    /**
     * 发送事件
     * 
     * @param topic 通过消息发送 empty spring event
     */
    public void emit(String topic, boolean async) {
        if (async) {
            AsyncTaskExecutor taskExecutor = SpringContextHolder.getBean(AsyncTaskExecutor.class);
            taskExecutor.execute(() -> {
                emit(topic, false);
            });
            return;
        }
        try {
            if (StringUtils.hasText(topic)) {
                // MessageProducer messageProducer =
                // SpringContextHolder.getBean(MessageProducer.class);
                // messageProducer.send(this, topic);
            } else {
                SpringContextHolder.publishEvent(this);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    protected static String useDefaultTopic(Class<?> clazz) {
        String useTopic = null;
        if (defaultTopicMap.containsKey(clazz)) {
            defaultTopicMap.get(clazz);
        } else {
            Topic topicAnn = clazz.getAnnotation(Topic.class);
            if (topicAnn != null) {
                useTopic = (String) SpringContextHolder.resolveExpression(topicAnn.value());
            }
        }
        return useTopic;
    }

    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Topic {
        String value();
    }

}