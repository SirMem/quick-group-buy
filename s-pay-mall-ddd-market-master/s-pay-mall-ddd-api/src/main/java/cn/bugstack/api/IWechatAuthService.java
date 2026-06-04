package cn.bugstack.api;

import cn.bugstack.api.dto.WechatPollResponseDTO;
import cn.bugstack.api.dto.WechatQrCodeResponseDTO;
import cn.bugstack.api.response.Response;

public interface IWechatAuthService {

    Response<WechatQrCodeResponseDTO> createQrCode();

    Response<WechatPollResponseDTO> poll(String qrCodeId);

}
