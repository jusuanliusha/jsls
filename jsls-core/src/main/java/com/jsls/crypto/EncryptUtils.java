package com.jsls.crypto;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.jsls.util.DateUtils;

public class EncryptUtils {
    private static final Logger logger=LoggerFactory.getLogger(EncryptUtils.class);
    public static class XORInputStream extends FilterInputStream {
		private final byte[] xorFactor;
		private int i = 0;

		protected XORInputStream(InputStream in, byte[] xorFactor) {
			super(in);
			this.xorFactor = xorFactor;
		}

		@Override
		public int read() throws IOException {
			int original=super.read();
			if(original==-1){
				return original;
			}
			int ui = i++ % xorFactor.length;
			return  original^(byte)xorFactor[ui];
		}
		@Override
		public int read(byte b[], int off, int len) throws IOException {
			len=in.read(b, off, len);
			for(int j=0;j<len;j++){
				int ui = i++ % xorFactor.length;
				byte bi=b[off+j];
				b[off+j]=(byte)(bi^(byte)xorFactor[ui]);
			}
			return len;
		}
	}

	public static class XOROutputStream extends FilterOutputStream {
		private final byte[] xorFactor;
		private int i = 0;

		protected XOROutputStream(OutputStream out, byte[] xorFactor) {
			super(out);
			this.xorFactor = xorFactor;
		}

		@Override
		public void write(int b) throws IOException {
			int ui = i++ % xorFactor.length;
			super.write(b ^ (byte)xorFactor[ui]);
		}
	}

	public static XORInputStream useXOR(InputStream in, String key) {
		Assert.hasText(key, "key不能为空！");
		try {
			return new XORInputStream(in, key.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static XOROutputStream useXOR(OutputStream out, String key) {
		Assert.hasText(key, "key不能为空！");
		try {
			return new XOROutputStream(out, key.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}

	public static CipherInputStream useSM4(InputStream in, String key, String iv) {
		try {
			return new CipherInputStream(in, SM4Utils.useCipher(key, iv, false));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}

	public static CipherOutputStream useSM4(OutputStream out, String key, String iv) {
		try {
			return new CipherOutputStream(out, SM4Utils.useCipher(key, iv, true));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}

	public static CipherInputStream useAES(InputStream in, String key, String iv) {
		try {
			return new CipherInputStream(in, AESUtils.useCipher(key, iv, false));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}

	public static CipherOutputStream useAES(OutputStream out, String key, String iv) {
		try {
			return new CipherOutputStream(out, AESUtils.useCipher(key, iv, true));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}

	public static CipherInputStream useDES(InputStream in, String key, String iv) {
		try {
			return new CipherInputStream(in, DESUtils.useCipher(key, iv, false));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}

	public static CipherOutputStream useDES(OutputStream out, String key, String iv) {
		try {
			return new CipherOutputStream(out, DESUtils.useCipher(key, iv, true));
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}
    /**
     * 验证签名
     * 
     * @param publicKey
     * @param text
     * @param sign
     * @return
     */
    public static boolean verify(String publicKey, String text, String sign) {
        try {
            if(RSAUtils.verify(publicKey, text.getBytes("utf-8"), sign)){
                return true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("publicKey:"+publicKey);
        logger.info("text:"+text);
        logger.info("sign:"+sign);
        return false;
    }
    /**
     * 签名
     * @param privateKey 私钥
     * @param content 内容
     * @return
     * @throws Exception
     */
    public static String sign(String privateKey, String content)throws Exception {
        return RSAUtils.sign(privateKey, content.getBytes("utf-8"));
    }
    /**
     * 签名
     * @param privateKey 私钥
     * @param orderMap 排序的Map
     * @return
     * @throws Exception
     */
    public static String sign(String privateKey, Map<String, Object> orderMap)throws Exception {
        return sign(privateKey, buildForSign(orderMap));
    }

    @SuppressWarnings("unchecked")
    public static String buildForSign(Map<String,Object> orderMap){
        int i=0;
        StringBuilder sb=new StringBuilder();
        for(Map.Entry<String,Object> entry:orderMap.entrySet()){
            String key=entry.getKey();
            Object value=entry.getValue();
            if(value==null||!StringUtils.hasText(key)){
                continue;
            }
            if(++i>1){
                sb.append("&");
            }
            sb.append(key).append("=");
            if(value instanceof Collection<?>){
                buildJSON((Collection<?>)value, sb);
            }else if(value instanceof Object[]){
                buildJSON((Object[])value,sb);
            }else if(value instanceof Map){
                buildJSON((Map<String,Object>) value, sb);
            }else{
                sb.append(value);
            }
        }
        return sb.toString();
    }
    private static void buildJSON(Map<String,Object> orderMap,StringBuilder sb){
        int i=0;
        sb.append("{");
        for(Map.Entry<String,Object> entry:orderMap.entrySet()){
            String key=entry.getKey();
            Object value=entry.getValue();
            if(value==null||!StringUtils.hasText(key)){
                continue;
            }
            if(++i>1){
                sb.append(",");
            }
            sb.append("\"").append(key).append("\"").append(":");
            buildJSON(value,sb);
        }
        sb.append("}");
    }
    private static void buildJSON(Collection<?> values,StringBuilder sb){
        sb.append("[");
        int i=0;
        for(Object item:values){
            if(++i>1){
                sb.append(",");
            }
            buildJSON(item,sb);
        }
        sb.append("]");
    }
    private static void buildJSON(Object[] values,StringBuilder sb){
        sb.append("[");
        int i=0;
        for(Object item:values){
            if(++i>1){
                sb.append(",");
            }
            buildJSON(item,sb);
        }
        sb.append("]");
    }
    @SuppressWarnings("unchecked")
    private static void buildJSON(Object value,StringBuilder sb){
        if(value==null){
            sb.append("null");
        }else if(value instanceof Number){
            sb.append(value);
        }else if(value instanceof Date){
            sb.append("\"").append(DateUtils.formatDate((Date)value, DateUtils.DATE_FORMAT_DATETIME)).append("\"");
        }else if(value instanceof Object[]){
            buildJSON((Object[])value,sb);
        }else if(value instanceof Collection<?>){
            buildJSON((Collection<?>)value, sb);
        }else if(value instanceof Map){
            buildJSON((Map<String,Object>)value, sb);
        }else{
            sb.append("\"").append(value).append("\"");
        }
    }
    /**
     * 私钥加密
     *
     * @param privateKey 私钥
     * @param text 待加密的信息
     * @return /
     * @throws Exception /
     */
    public static String rsaPrivateKeyEncrypt(String privateKey, String text) throws Exception {
        return RSAUtils.encrypt(RSAUtils.loadPrivateKeyByStr(privateKey), text.getBytes("utf-8"));
    }
    /**
     * 公钥加密
     *
     * @param publicKey 公钥
     * @param text 待加密的文本
     * @return /
     */
    public static String rsaPublicKeyEncrypt(String publicKey, String text) throws Exception {
        return RSAUtils.encrypt(RSAUtils.loadPublicKeyByStr(publicKey), text.getBytes("utf-8"));
    }
    /**
     * RSA私钥解密
     * 
     * @param privateKey
     * @param encryptText
     * @return
     */
    public static String rsaPrivateKeyDecrypt(String privateKey, String encryptText) throws Exception{
        byte[] plainData = RSAUtils.decrypt(RSAUtils.loadPrivateKeyByStr(privateKey), encryptText);
        return new String(plainData, "utf-8");
    }
    /**
     * RSA公钥解密
     * 
     * @param publicKey
     * @param encryptText
     * @return
     */
    public static String rsaPublicKeyDecrypt(String publicKey, String encryptText) throws Exception{
        byte[] plainData = RSAUtils.decrypt(RSAUtils.loadPublicKeyByStr(publicKey), encryptText);
        return new String(plainData, "utf-8");
    }
    /**
     * AES解密
     * @param encryptKey
     * @param encryptText
     * @param ivParameter
     * @return
     */
    public static String aesEncrypt(String encryptKey, String text,String ivParameter) throws Exception{
        return AESUtils.encrypt(encryptKey, text,ivParameter);
    }
    /**
     * AES解密
     * @param encryptKey
     * @param encryptText
     * @param ivParameter
     * @return
     */
    public static String aesDecrypt(String encryptKey, String encryptText,String ivParameter) throws Exception{
        return AESUtils.decrypt(encryptKey, encryptText,ivParameter);
    }
    /**
     * DES解密
     * @param encryptKey
     * @param encryptText
     * @param ivParameter
     * @return
     */
    public static String desEncrypt(String encryptKey, String text,String ivParameter) throws Exception{
        return DESUtils.encrypt(encryptKey, text,ivParameter);
    }
    /**
     * DES解密
     * @param encryptKey
     * @param encryptText
     * @param ivParameter
     * @return
     */
    public static String desDecrypt(String encryptKey, String encryptText,String ivParameter) throws Exception{
        return DESUtils.decrypt(encryptKey, encryptText,ivParameter);
    }
}