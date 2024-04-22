package com.jsls.core;

import java.util.Date;

public class Timers {

    private static long lastTime;

    public static Date uniqueDate() {
        return new Date(uniqueTime());
    }

    public synchronized static long uniqueTime() {
        long time = System.currentTimeMillis();
        if (time <= lastTime) {
            time = lastTime + 1;
        }
        lastTime = time;
        return time;
    }
}
