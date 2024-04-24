package com.jsls.account.merchant;

import java.util.Map;
import java.util.TreeMap;

import com.jsls.core.Result;
import com.jsls.crypto.EncryptUtils;
import com.jsls.crypto.RSAUtils;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(value = "返回商户结果")
public class MerchantResult<D> extends Result<D> {
    @ApiModelProperty(value = "签名")
    private String sign;
    public void applySign(String merchantSecret, String merchantKeyPath) {
        Map<String, Object> orderMap = new TreeMap<String, Object>();
        orderMap.put("code", getCode());
        orderMap.put("message", getMessage());
        orderMap.put("success", isSuccess());
        D data=getData();
        if(data instanceof Signable){
            Map<String, Object> dataMap = new TreeMap<String, Object>();
            ((Signable)data).applyForSign(dataMap);
            orderMap.put("data", dataMap);
        }else{
            orderMap.put("data", data);
        }
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
