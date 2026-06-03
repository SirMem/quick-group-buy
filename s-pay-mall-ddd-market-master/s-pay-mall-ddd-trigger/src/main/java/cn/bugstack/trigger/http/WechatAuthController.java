package cn.bugstack.trigger.http;

import cn.bugstack.api.IWechatAuthService;
import cn.bugstack.api.dto.WechatPollResponseDTO;
import cn.bugstack.api.dto.WechatQrCodeResponseDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.auth.model.entity.WechatPollEntity;
import cn.bugstack.domain.auth.model.entity.WechatQrCodeEntity;
import cn.bugstack.domain.auth.service.ILoginService;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/auth/wechat/")
public class WechatAuthController implements IWechatAuthService {

    @Resource
    private ILoginService loginService;

    @PostMapping("qrcode")
    @Override
    public Response<WechatQrCodeResponseDTO> createQrCode() {
        try {
            WechatQrCodeEntity qrCodeEntity = loginService.createWechatQrCode();
            log.info("生成微信扫码登录二维码 qrCodeId:{} expiresIn:{}", qrCodeEntity.getQrCodeId(), qrCodeEntity.getExpiresIn());

            return Response.<WechatQrCodeResponseDTO>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(WechatQrCodeResponseDTO.builder()
                            .qrCodeId(qrCodeEntity.getQrCodeId())
                            .qrCodeUrl(qrCodeEntity.getQrCodeUrl())
                            .expiresIn(qrCodeEntity.getExpiresIn())
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("生成微信扫码登录二维码失败", e);
            return Response.<WechatQrCodeResponseDTO>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @GetMapping("poll")
    @Override
    public Response<WechatPollResponseDTO> poll(@RequestParam(required = false) String qrCodeId) {
        if (StringUtils.isBlank(qrCodeId)) {
            return Response.<WechatPollResponseDTO>builder()
                    .code(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info(Constants.ResponseCode.ILLEGAL_PARAMETER.getInfo())
                    .build();
        }

        try {
            WechatPollEntity pollEntity = loginService.pollWechatLogin(qrCodeId);
            log.info("查询微信扫码登录状态 qrCodeId:{} status:{}", qrCodeId, pollEntity.getStatus());

            return Response.<WechatPollResponseDTO>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(WechatPollResponseDTO.builder()
                            .status(pollEntity.getStatus())
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("查询微信扫码登录状态失败 qrCodeId:{}", qrCodeId, e);
            return Response.<WechatPollResponseDTO>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
