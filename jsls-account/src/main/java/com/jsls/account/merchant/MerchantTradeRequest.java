package com.jsls.account.merchant;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel("商户交易请求")
public class MerchantTradeRequest extends MerchantRequest {
    @ApiModelProperty("用于加密业务参数报文的密钥密文,原文为随机数或随机字符串,建议不要带特殊字符")
    private String encryptKey;
    @ApiModelProperty("带签文的业务参数报文的密文,使用密钥(encryptKey)做AES加密")
    private String encryptData;

    @Override
    public void applyRule(Rule rule, String mode) {
        super.applyRule(rule, mode);
        rule.hasText(encryptKey, "encryptKey不能为空！");
        rule.hasText(encryptData, "encryptData不能为空！");
    }
}
