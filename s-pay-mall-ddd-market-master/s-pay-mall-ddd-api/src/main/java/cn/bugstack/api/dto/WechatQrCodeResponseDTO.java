package cn.bugstack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WechatQrCodeResponseDTO {

    private String qrCodeId;
    private String qrCodeUrl;
    private Long expiresIn;

}
