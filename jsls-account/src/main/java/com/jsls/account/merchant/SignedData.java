package com.jsls.account.merchant;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.util.StringUtils;

import com.jsls.core.Result;
import com.jsls.core.Verifiable;
import com.jsls.crypto.EncryptUtils;
import com.jsls.crypto.RSAUtils;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class SignedData implements Signable,Verifiable{

    @ApiModelProperty("签名")
    private String sign;

    @Override
    public void applyRule(Rule rule, String mode) {
        if(!"BASE".equals(mode)){
            rule.hasText(sign, "sign不能为空！");
        }
    }
    
    public boolean filter(PropertyDescriptor pd){
        Method  method =pd.getReadMethod();
        if(method==null){
            return false;
        }
        Class<?> dClass=method.getDeclaringClass();
        if(Object.class.equals(dClass)||SignedData.class.equals(dClass)){
            return false;
        }
        return true;
    }

    public Result<Void> validate(String publicKey,String merchantSecret){
        Result<Void> r=validate();
        if(!r.isSuccess()){
            return r;
        }
        String content=useContentForSign();
        if(StringUtils.hasText(merchantSecret)){
            content+=merchantSecret;
        }
        if(!EncryptUtils.verify(publicKey, content, sign)){
            return Result.fail("验证签名失败！");
        }
        return Result.SUCCESS;
    }
    public void applySign(String merchantSecret, String merchantKeyPath) {
        Map<String, Object> orderMap = new TreeMap<String, Object>();
        this.applyForSign(orderMap);
        String content = EncryptUtils.buildForSign(orderMap) + (merchantSecret != null ? merchantSecret : "");
        String privateKey;
        try {
            privateKey = RSAUtils.loadPrivateKeyByFile(merchantKeyPath);
            String sign = EncryptUtils.sign(privateKey, content);
            this.setSign(sign);
        } catch (Exception e) {
            throw new RuntimeException("签名异常："+e.getMessage(),e);
        }
    }
}
