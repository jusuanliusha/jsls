package com.jsls.core;

import java.util.Date;

import org.springframework.util.StringUtils;

import com.jsls.util.DateUtils;

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

    /**
     * 瞬时名称,通过加"-${timestamp}"后缀保证唯一性
     * <p>
     * 1.如果name包含.则name被当做文件，时间错加在扩展名的前面
     * 
     * @param name
     * @return
     */
    public static String useTimestampName(String name) {
        return useTimestampName(name, null);
    }

    /**
     * 瞬时名称,通过加"-${timestamp}"后缀保证唯一性
     * <p>
     * 1.如果name包含.则name被当做文件，时间错加在扩展名的前面
     * <p>
     * 2.还可以通过传prefix给名称加前缀
     * 
     * @param name
     * @param prefix
     * @return
     */
    public static String useTimestampName(String name, String prefix) {
        String timer = DateUtils.formatDate(Timers.uniqueDate(), "yyyyMMddHHmmssSSS");
        boolean filePrefix = false;
        if (!StringUtils.hasText(prefix)) {
            filePrefix = true;
            prefix = name;
        }
        String suffix = "";
        int lastDotIndex = name.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            if (filePrefix) {
                prefix = name.substring(0, lastDotIndex);
            }
            suffix = name.substring(lastDotIndex).toLowerCase();
        }
        return prefix + "-" + timer + suffix;
    }
}
