package com.jsls.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.StringUtils;

public class DESUtils {
    public static final String ALGORITHM = "DES";
    public static final String ALGORITHM_CBC_PADDING = ALGORITHM + "/CBC/PKCS5Padding";

    /**
     * DES加密
     * 
     * @param encryptKey 密钥
     * @param content    内容
     * @return
     * @throws Exception
     */
    public static String encrypt(String encryptKey, String content, String ivParameter) throws Exception {
        byte[] bytes = encrypt(encryptKey, content.getBytes("UTF-8"), ivParameter);
        return Base64Utils.encodeBase64String(bytes);
    }

    public static byte[] encrypt(String encryptKey, byte[] content, String ivParameter) throws Exception {
        // 加密 1.构造密钥 2.创建和初始化密码器 3.内容加密 4.返回字符串
        Cipher cipher = useCipher(encryptKey, ivParameter, true);
        return cipher.doFinal(content);
    }

    /**
     * DES解密
     * 
     * @param encryptKey  密钥
     * @param encryptText 密文
     * @param ivParameter
     * @return
     * @throws Exception
     */
    public static String decrypt(String encryptKey, String encryptText, String ivParameter) throws Exception {
        byte[] bytes = decrypt(encryptKey, Base64Utils.decodeBase64(encryptText), ivParameter);
        return new String(bytes, "UTF-8");
    }

    /**
     * DES解密
     * 
     * @param encryptKey  密钥
     * @param data        密文
     * @param ivParameter
     * @return
     * @throws Exception
     */
    public static byte[] decrypt(String encryptKey, byte[] data, String ivParameter) throws Exception {
        // 解密 1.构造密钥 2.创建和初始化密码器 3.将加密后的字符串反纺成byte[]数组 4.将加密内容解密
        Cipher cipher = useCipher(encryptKey, ivParameter, false);
        return cipher.doFinal(data);
    }

    /**
     * DES加密算法
     * 
     * @param encryptKey
     * @param ivParameter
     * @param encrypt
     * @return
     * @throws Exception
     */
    public static Cipher useCipher(String encryptKey, String ivParameter, boolean encrypt) throws Exception {
        int encryptMode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        SecretKey key = new SecretKeySpec(encryptKey.getBytes(), ALGORITHM);
        if (StringUtils.hasText(ivParameter)) {
            Cipher cipher = Cipher.getInstance(ALGORITHM_CBC_PADDING);
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
            cipher.init(encryptMode, key, iv);
            return cipher;
        }
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(encryptMode, key);
        return cipher;
    }
}
