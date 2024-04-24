package com.jsls.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomUtils {
    private static final Logger logger=LoggerFactory.getLogger(RandomUtils.class);
    public static final String ALLCHAR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LETTERCHAR = "abcdefghijkllmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String NUMBERCHAR = "0123456789";

    /**
     * 返回一个定长的随机字符串(只包含大小写字母、数字)
     * 
     * @param length 随机字符串长度
     * @return 随机字符串
     */
    public static String generateString(int length) {
        StringBuilder sb = new StringBuilder();
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
        }
        for (int i = 0; i < length; i++) {
            char tmp = (char) (33 + random.nextInt(93));
            while (tmp == 92) {
                tmp = (char) (33 + random.nextInt(93));
            }
            sb.append(tmp);
        }
        return sb.toString();
    }

    /**
     * 返回一个定长的随机字符串(只包含数字)
     * 
     * @param length 随机字符串长度
     * @return 随机字符串
     */
    public static String generateDigital(int length) {
        StringBuilder sb = new StringBuilder();
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
        }
        for (int i = 0; i < length; i++) {
            sb.append(NUMBERCHAR.charAt(random.nextInt(NUMBERCHAR.length())));
        }
        return sb.toString();
    }
}