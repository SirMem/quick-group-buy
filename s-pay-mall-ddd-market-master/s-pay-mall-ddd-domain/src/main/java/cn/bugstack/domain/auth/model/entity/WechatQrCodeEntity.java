package cn.bugstack.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WechatQrCodeEntity {

    private String qrCodeId;
    private String qrCodeUrl;
    private Long expiresIn;

}
