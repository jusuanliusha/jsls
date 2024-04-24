package com.jsls.account.merchant;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class TradeData extends SignedData {
    @ApiModelProperty("时间戳")
    private String timestamp;
    @Override
    public void applyRule(Rule rule, String mode) {
        super.applyRule(rule, mode);
        rule.hasText(timestamp, "timestamp不能为空！");
    }
}
