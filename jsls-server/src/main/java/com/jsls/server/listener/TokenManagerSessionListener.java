package com.jsls.server.listener;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenManagerSessionListener implements HttpSessionListener {
    private static final Logger logger = LoggerFactory.getLogger(TokenManagerSessionListener.class);

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        String sid = sessionEvent.getSession().getId();
        logger.info("session {} created", sid);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        String sid = sessionEvent.getSession().getId();
        logger.info("session {} destroyed", sid);
    }
}
