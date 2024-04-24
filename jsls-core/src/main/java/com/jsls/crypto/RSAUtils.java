package com.jsls.crypto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * RSA工具类
 */
public class RSAUtils {
    /**
     * 签名算法
     */
    public static final String SIGN_ALGORITHMS = "SHA1WithRSA";
    /**
     * 字节数据转字符串专用集合
     */
    private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f' };

    /**
     * 随机生成密钥对
     * 
     * @param filePath
     */
    public static void genKeyPair(String filePath) {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // 初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(2048, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        PrivateKey privateKey = keyPair.getPrivate();
        // 得到公钥
        PublicKey publicKey = keyPair.getPublic();
        try {
            // 得到公钥字符串
            String publicKeyString = Base64Utils.encodeBase64String(publicKey.getEncoded());
            // 得到私钥字符串
            String privateKeyString = Base64Utils.encodeBase64String(privateKey.getEncoded());
            // 将密钥对写入到文件
            FileWriter pubfw = new FileWriter(filePath + "/publicKey.keystore");
            FileWriter prifw = new FileWriter(filePath + "/privateKey.keystore");
            BufferedWriter pubbw = new BufferedWriter(pubfw);
            BufferedWriter pribw = new BufferedWriter(prifw);
            pubbw.write(publicKeyString);
            pubbw.flush();
            pubbw.close();
            pubfw.close();
            pribw.write(privateKeyString);
            pribw.flush();
            pribw.close();
            prifw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static PrivateKey loadPrivateKeyFromPem(File file) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String s = br.readLine();
		String str = "";
		s = br.readLine();
		while (s.charAt(0) != '-') {
			str += s;
			s = br.readLine();
		}
		br.close();
		byte[] b = Base64Utils.decodeBase64(str);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(b);
		PrivateKey privateKey = kf.generatePrivate(keySpec);
		return privateKey;
	}
	
	public static PublicKey loadPublicKeyFromPem(File file) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String s = br.readLine();
		String str = "";
		s = br.readLine();
		while (s.charAt(0) != '-') {
			str += s;
			s = br.readLine();
		}
		br.close();
		byte[] b = Base64Utils.decodeBase64(str);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(b);
		PublicKey pubKey = kf.generatePublic(keySpec);
		return pubKey;
	}
    /**
     * 从文件中输入流中加载公钥
     * 
     * @param in 公钥输入流
     * @throws Exception 加载公钥时产生的异常
     */
    public static String loadPublicKeyByFile(String path) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path + "/publicKey.keystore"));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                sb.append(readLine);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            throw new Exception("公钥数据流读取错误");
        } catch (NullPointerException e) {
            throw new Exception("公钥输入流为空");
        }
    }

    /**
     * 从字符串中加载公钥
     * 
     * @param publicKeyStr 公钥数据字符串
     * @throws Exception 加载公钥时产生的异常
     */
    public static PublicKey loadPublicKeyByStr(String publicKeyStr) throws Exception {
        try {
            byte[] buffer = Base64Utils.decodeBase64(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("公钥非法");
        } catch (NullPointerException e) {
            throw new Exception("公钥数据为空");
        }
    }

    /**
     * 从文件中加载私钥
     * 
     * @param keyFileName 私钥文件名
     * @return 是否成功
     * @throws Exception
     */
    public static String loadPrivateKeyByFile(String path) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path + "/privateKey.keystore"));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                sb.append(readLine);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            throw new Exception("私钥数据读取错误");
        } catch (NullPointerException e) {
            throw new Exception("私钥输入流为空");
        }
    }

    public static PrivateKey loadPrivateKeyByStr(String privateKeyStr) throws Exception {
        try {
            byte[] buffer = Base64Utils.decodeBase64(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("私钥非法");
        } catch (NullPointerException e) {
            throw new Exception("私钥数据为空");
        }
    }

    /**
     * 公钥加密过程
     * 
     * @param publicKey     公钥
     * @param plainTextData 明文数据
     * @return
     * @throws Exception 加密过程中的异常信息
     */
    public static String encrypt(PublicKey publicKey, byte[] plainTextData) throws Exception {
        if (publicKey == null) {
            throw new Exception("加密公钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] output = cipher.doFinal(plainTextData);
            return Base64Utils.encodeBase64String(output);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此加密算法");
        } catch (NoSuchPaddingException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw new Exception("加密公钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("明文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("明文数据已损坏");
        }
    }

    /**
     * 私钥加密过程
     * 
     * @param privateKey    私钥
     * @param plainTextData 明文数据
     * @return
     * @throws Exception 加密过程中的异常信息
     */
    public static String encrypt(PrivateKey privateKey, byte[] plainTextData) throws Exception {
        if (privateKey == null) {
            throw new Exception("加密私钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            // 使用默认RSA
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] output = cipher.doFinal(plainTextData);
            return Base64Utils.encodeBase64String(output);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此加密算法");
        } catch (NoSuchPaddingException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw new Exception("加密私钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("明文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("明文数据已损坏");
        }
    }

    /**
     * 私钥解密过程
     * 
     * @param privateKey 私钥
     * @param cipherData 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public static byte[] decrypt(PrivateKey privateKey, String cipherText) throws Exception {
        if (privateKey == null) {
            throw new Exception("解密私钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            // 使用默认RSA
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] output = cipher.doFinal(Base64Utils.decodeBase64(cipherText));
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此解密算法");
        } catch (NoSuchPaddingException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw new Exception("解密私钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("密文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("密文数据已损坏");
        }
    }

    /**
     * 公钥解密过程
     * 
     * @param publicKey  公钥
     * @param cipherText 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public static byte[] decrypt(PublicKey publicKey, String cipherText) throws Exception {
        if (publicKey == null) {
            throw new Exception("解密公钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] output = cipher.doFinal(Base64Utils.decodeBase64(cipherText));
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此解密算法");
        } catch (NoSuchPaddingException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw new Exception("解密公钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("密文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("密文数据已损坏");
        }
    }

    /**
     * RSA签名
     * 
     * @param privateKey 商户私钥
     * @param content    待签名数据
     * @return 签名值
     */
    public static String sign(String privateKey, byte[] content) throws Exception {
        PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64Utils.decodeBase64(privateKey));
        KeyFactory keyf = KeyFactory.getInstance("RSA");
        PrivateKey priKey = keyf.generatePrivate(priPKCS8);
        java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
        signature.initSign(priKey);
        signature.update(content);
        byte[] signed = signature.sign();
        return Base64Utils.encodeBase64String(signed);
    }

    /**
     * RSA验签名检查
     * 
     * @param publicKey
     * @param content
     * @param sign
     * @return
     * @throws Exception
     */
    public static boolean verify(String publicKey, byte[] content, String sign) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] encodedKey = Base64Utils.decodeBase64(publicKey);
        PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
        java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
        signature.initVerify(pubKey);
        signature.update(content);
        return signature.verify(Base64Utils.decodeBase64(sign));
    }

    /**
     * 字节数据转十六进制字符串
     * 
     * @param data 输入数据
     * @return 十六进制内容
     */
    public static String byteArrayToString(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            // 取出字节的高四位 作为索引得到相应的十六进制标识符 注意无符号右移
            stringBuilder.append(HEX_CHAR[(data[i] & 0xf0) >>> 4]);
            // 取出字节的低四位 作为索引得到相应的十六进制标识符
            stringBuilder.append(HEX_CHAR[(data[i] & 0x0f)]);
            if (i < data.length - 1) {
                stringBuilder.append(' ');
            }
        }
        return stringBuilder.toString();
    }
}