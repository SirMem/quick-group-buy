package cn.bugstack.domain.auth.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefreshTokenStatusVO {

    ACTIVE("ACTIVE", "有效"),
    USED("USED", "已被刷新换新"),
    REVOKED("REVOKED", "已吊销");

    private final String code;
    private final String info;

}
