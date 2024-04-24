package com.jsls.account.merchant;

import com.jsls.core.Verifiable;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class MerchantRequest implements Verifiable {
     @ApiModelProperty("商户号")
     private String merchantNo;

     @Override
     public void applyRule(Rule rule, String mode) {
          rule.hasText(merchantNo, "merchantNo不能为空！");
     }
}
