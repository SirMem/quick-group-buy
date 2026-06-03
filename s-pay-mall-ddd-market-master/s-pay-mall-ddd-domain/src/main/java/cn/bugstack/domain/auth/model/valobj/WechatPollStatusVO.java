package cn.bugstack.domain.auth.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WechatPollStatusVO {

    WAITING("WAITING", "等待用户扫码"),
    SCANNED("SCANNED", "用户已扫码，但尚未返回双 Token"),
    CONFIRMED("CONFIRMED", "用户已确认登录"),
    EXPIRED("EXPIRED", "二维码已过期");

    private final String code;
    private final String info;

}
