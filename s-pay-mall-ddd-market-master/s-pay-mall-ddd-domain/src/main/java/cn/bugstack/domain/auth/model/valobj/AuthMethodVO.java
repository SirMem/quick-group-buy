package cn.bugstack.domain.auth.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthMethodVO {

    WECHAT_QR("WECHAT_QR", "微信扫码登录");

    private final String code;
    private final String info;

}
