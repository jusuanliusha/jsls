package com.jsls.crypto;

public class Base64Utils {
    public static byte[] decodeBase64(String base64){
        return org.springframework.util.Base64Utils.decodeFromString(base64);
    }
    public static String encodeBase64String(byte[] bytes){
        return org.springframework.util.Base64Utils.encodeToString(bytes);
    }
}