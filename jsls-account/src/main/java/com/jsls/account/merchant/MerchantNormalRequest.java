package com.jsls.account.merchant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MerchantNormalRequest<D extends SignedData> extends MerchantRequest {
    @ApiModelProperty("数据")
    private D data;

    @Override
    public void applyRule(Rule rule, String mode) {
        super.applyRule(rule, mode);
        if(rule.notNull(data, "data不能为空！")){
            data.applyRule(rule, mode);
        }
    }
}
